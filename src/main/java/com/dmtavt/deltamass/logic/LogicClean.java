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
    if (cmd.dryRun) log.info("Dry-run, won't take any action.");
    int stepsPerformed = 0;

    if (!cmd.noPep) {
      stepsPerformed++;
      log.info("Cleaning up peptide identification files.");
      DeletionStats stats = deleteMatchingFilesInSubtrees(cmd.inputFiles,
          CacheLocator::isPepCacheFile, cmd.dryRun);
      log.info("Pep ID cleanup summary: scheduled: {}, deleted: {}, skipped: {}, error: {}",
          stats.scheduled, stats.deleted, stats.skipped.size(), stats.error.size());
    }

    if (!cmd.noLcms) {
      stepsPerformed++;
      log.info("Cleaning up LC-MS calibration files.");
      DeletionStats stats = deleteMatchingFilesInSubtrees(cmd.inputFiles,
          CacheLocator::isCalCacheFile, cmd.dryRun);
      log.info("Calibration cleanup summary: scheduled: {}, deleted: {}, skipped: {}, error: {}",
          stats.scheduled, stats.deleted, stats.skipped.size(), stats.error.size());
    }

    if (stepsPerformed == 0) log.info("You made it clear that nothing should be cleaned up. Doing nothing.");
  }

  public List<Path> getScheduledForDeletion() {
    List<Path> delete = new ArrayList<>();
    if (!cmd.noPep) {
      delete.addAll(getScheduledForDeletion(cmd.inputFiles, CacheLocator::isPepCacheFile));
    }
    if (!cmd.noLcms) {
      delete.addAll(getScheduledForDeletion(cmd.inputFiles, CacheLocator::isCalCacheFile));
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
        if (condition.test(path)) {
          delete.add(path);
        }
      }
    }
    return new ArrayList<>(delete);
  }

  private class DeletionStats {
    int scheduled = 0;
    int deleted = 0;
    ConcurrentLinkedQueue<Path> skipped = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Path> error = new ConcurrentLinkedQueue<>();
  }

  private DeletionStats deleteMatchingFilesInSubtrees(Iterable<? extends Path> paths, Predicate<? super Path> condition, boolean dryRun) {
    final ConcurrentLinkedQueue<Path> delete = new ConcurrentLinkedQueue<>();
    final DeletionStats stats = new DeletionStats();

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
                stats.skipped.add(file);
                return FileVisitResult.CONTINUE;
              }
              if (condition.test(file)) delete.add(file);
              return FileVisitResult.CONTINUE;
            }
          });
        } catch (IOException e) {
          log.error("Error collecting files for deletion in subtree: {}" + path, e);
        }
      } else {
        if (CacheLocator.isPepCacheFile(path))
          delete.add(path);
      }
    }

    if (!delete.isEmpty()) {
      log.info("Files scheduled for deletion:\n    {}",
          delete.stream().map(Path::toString).collect(Collectors.joining("\n    ")));
    } else {
      log.info("Found no files to delete.");
    }
    stats.scheduled = delete.size();

    if (!dryRun) delete.forEach(path -> {
      try {
        Files.deleteIfExists(path);
        stats.deleted++;
      } catch (IOException e) {
        log.error("Could not delete file: " + path, e);
        stats.error.add(path);
      }
    });

    return stats;
  }
}
