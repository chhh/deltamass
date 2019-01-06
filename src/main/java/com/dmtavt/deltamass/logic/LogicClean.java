package com.dmtavt.deltamass.logic;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicClean {

  private static final Logger log = LoggerFactory.getLogger(LogicClean.class);
  final CommandClean cmd;

  public LogicClean(CommandClean cmd) {
    this.cmd = cmd;
  }

  public void run() {
    log.info("Running cleanup");
    final List<Path> scheduledForDeletion = getScheduledForDeletion();

    final String list = scheduledForDeletion.stream().map(Path::toString)
        .collect(Collectors.joining("\n"));
    log.info("Files to delete:\n" + list);

    if (cmd.dryRun) {
      log.info("Dry-run, won't take any action.");
      return;
    }

    DeletionStats stats = delete(scheduledForDeletion);
    log.info("Cleanup: deleted {}/{}, errors: {}", stats.deleted.size(), stats.scheduled.size(), stats.error.size());
  }

  private static List<Path> modifyInputPathsForCacheDeletion(List<Path> paths) {
    List<Path> possibleCachePaths = new ArrayList<>();
    for (Path inputFile : paths) {
      if (Files.isDirectory(inputFile)) {
        possibleCachePaths.add(inputFile);
      } else {
        Path parent = inputFile.getParent();
        Path fn = inputFile.getFileName();
        possibleCachePaths.add(parent.resolve(fn.toString() + CacheLocator.CACHE_EXT_PEP));
        possibleCachePaths.add(parent.resolve(fn.toString() + CacheLocator.CACHE_EXT_CAL));
      }
    }
    return possibleCachePaths;
  }

  public List<Path> getScheduledForDeletion() {
    List<Path> delete = new ArrayList<>();
    final List<Path> putativeCachePaths = modifyInputPathsForCacheDeletion(cmd.inputFiles);

    if (!cmd.noPep) {
      delete.addAll(getScheduledForDeletion(putativeCachePaths, CacheLocator::isPepCacheFile));
    }
    if (!cmd.noLcms) {
      delete.addAll(getScheduledForDeletion(putativeCachePaths, CacheLocator::isCalCacheFile));
    }
    return delete;
  }

  private List<Path> getScheduledForDeletion(Iterable<? extends Path> paths, Predicate<? super Path> condition) {
    final ConcurrentLinkedQueue<Path> delete = new ConcurrentLinkedQueue<>();

    for (Path path : paths) {
      if (Files.isDirectory(path)) {
        try {
          Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (!Files.isReadable(dir)) return FileVisitResult.SKIP_SUBTREE;
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              if (!Files.isWritable(file)) {
                log.warn("File not writeable, skipping: {}", file);
                return FileVisitResult.CONTINUE;
              }
              if (condition.test(file)) {
                delete.add(file);
              }
              return FileVisitResult.CONTINUE;
            }
          });
        } catch (IOException e) {
          log.warn("Error collecting files for deletion in subtree: {}" + path, e);
        }
      } else {
        if (Files.exists(path) && condition.test(path)) {
          delete.add(path);
        }
      }
    }
    return new ArrayList<>(delete);
  }

  private static class DeletionStats {
    ConcurrentLinkedQueue<Path> scheduled = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Path> deleted = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Path> error = new ConcurrentLinkedQueue<>();
  }

  public static DeletionStats delete(List<Path> pathsToDelete) {
    DeletionStats ds = new DeletionStats();
    ds.scheduled.addAll(pathsToDelete);
    for (Path path : pathsToDelete) {
      try {
        if (Files.deleteIfExists(path)) {
          ds.deleted.add(path);
        }
      } catch (IOException e) {
        log.error("Error deleting file: " + path.toString() + "\nDetails:\n" + e.getMessage());
        ds.error.add(path);
      }
    }
    return ds;
  }

}
