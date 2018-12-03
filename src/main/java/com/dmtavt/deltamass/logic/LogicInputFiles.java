package com.dmtavt.deltamass.logic;

import static com.dmtavt.deltamass.utils.NumUtils.isGoodDouble;

import com.dmtavt.deltamass.args.DecoyTreatment;
import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.data.PepSearchFile;
import com.dmtavt.deltamass.data.PepSearchResult;
import com.dmtavt.deltamass.data.PepSearchResult.SortOrder;
import com.dmtavt.deltamass.parsers.IParserFactory;
import com.dmtavt.deltamass.parsers.IPepidParser;
import com.dmtavt.deltamass.parsers.PepidParserRegistry;
import com.dmtavt.deltamass.predicates.SpmPredicateFactory;
import com.dmtavt.deltamass.utils.NumUtils;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.exceptions.ParsingException;
import com.github.chhh.utils.files.FileListing;
import com.github.chhh.utils.ser.DataStoreUtils;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicInputFiles {
  private static final Logger log = LoggerFactory.getLogger(LogicInputFiles.class);
  final UserOptsInputFiles opts;

  private FoundFiles foundFiles;

  public LogicInputFiles(UserOptsInputFiles opts) {
    this.opts = opts;
  }

  private void checkInterrupt() {
    if (Thread.interrupted()) {
      throw new RuntimeException("Interrupted");
    }
  }

  public List<PepSearchFile> run() {
    List<Path> inputFiles;
    final boolean isRecursive = !opts.noRecurse;
    if (opts.fileRegex != null && !StringUtils.isNullOrWhitespace(opts.fileRegex.pattern())) {
      inputFiles = inputFiles(opts.inputFiles, opts.fileRegex, isRecursive);
      log.info("Total {} paths given or match file regular expression", inputFiles.size());
    } else {
      inputFiles = collectSupportedFiles(opts.inputFiles, isRecursive);
      log.info("Total {} found files in given paths without a regular expression to match against", inputFiles.size());
    }

    checkInterrupt();
    List<Path> supportedFiles = supportedFiles(inputFiles);
    log.info("Total {} files supported by installed parsers", supportedFiles.size());

    if (supportedFiles.isEmpty()) {
      log.info("No files to read.");
      return Collections.emptyList();
    }

    final FileReadingOpts fileReadingOpts = new FileReadingOpts(opts.decoyRegex);
    final MassCorrectionOpts massCorrOpts = new MassCorrectionOpts(
        opts.massCorrection, opts.additionalSearchPaths, opts.scoreNameForPsmSorting, opts.noCache);

    ConcurrentHashMap<Path, PepSearchFile> pepSearchFiles = new ConcurrentHashMap<>();
    Queue<Path> couldNotReadFiles = new ConcurrentLinkedQueue<>();


    foundFiles = new FoundFiles(inputFiles, supportedFiles, cachedFiles(supportedFiles));
    final List<Path> toLoad = foundFiles.supported.stream().sorted().collect(Collectors.toList());
    AtomicLong totalSpmsBeforeFiltering = new AtomicLong(0);
    AtomicLong totalSpmsAfterFiltering = new AtomicLong(0);

    log.info("Loading {} files\n    {}", toLoad.size(),
        toLoad.stream().map(path -> {
          if (!opts.noCache && foundFiles.cached.containsKey(path))
            return "[cached] " + path.toAbsolutePath().normalize().toString();
            return "[parse ] " + path.toAbsolutePath().normalize().toString();
        }).collect(Collectors.joining("\n    ")));

    List<String> descriptions = filterOptsDescription(opts);
    if (!descriptions.isEmpty()) {
      log.info("Filtering PSMs (rules follow):\n    {}", String.join("\n    ", descriptions));
    }

    // iterate over found files
    for (Path raw : toLoad) {
      checkInterrupt();
      PepSearchFile pepSearchFile = null;

      Path cached = opts.noCache ? null : foundFiles.cached.get(raw);
      if (cached != null) { // trying to read cached
        log.info("Cached file: {}", cached);
        try {
          pepSearchFile = readCached(cached);
        } catch (IOException e1) {
          log.error(String.format("Error loading cached file: %s,\n"
              + "    trying original instead: %s", cached, raw));
        }
      }

      if (pepSearchFile == null) { // no cache, parsing file
        log.info("Parsing file: {}", raw);
        try {
          pepSearchFile = readRaw(raw, fileReadingOpts);
        } catch (IOException e) {
          log.error("Could not read file: {}", raw);
          couldNotReadFiles.add(raw);
        }
        if (!opts.noCache && pepSearchFile != null) {
          try {
            Path cachePepPath = CacheLocator.locateForPep(raw);
            log.info("Writing cache: {}", cachePepPath);
            writeCached(cachePepPath, pepSearchFile);
          } catch (IOException e) {
            log.warn("Could not write cache file for: " + raw, e);
          }
        }
      }


      if (pepSearchFile == null)
        continue;


      // update Mass Correction for the loaded file
      boolean updated;
      try {
        updated = updateMassCorrection(pepSearchFile, massCorrOpts);
      } catch (IOException e) {
        log.error("Could not update mass correction for: {}", pepSearchFile.sourceFile);
        log.warn("Try specifying Additional search paths for LCMS files. 'Search paths' text field in GUI, "
            + "or '--search-path' command-line option.");
        throw new IllegalStateException("Mass correction could not be updated.", e);
      }
      if (!opts.noCache && updated) {
        final Path cachePepPath = CacheLocator.locateForPep(raw);
        try {
          log.info("Mass correction changed, updating cache: {}", cachePepPath);
          writeCached(cachePepPath, pepSearchFile);
        } catch (IOException e) {
          log.warn("Could not update cache file for: " + raw, e);
        }
      }


      // filter the file
      int spmsBeforeFiltering = pepSearchFile.pepSearchResults.stream()
          .mapToInt(pepSearchResult -> pepSearchResult.spms.size()).sum();
      totalSpmsBeforeFiltering.addAndGet(spmsBeforeFiltering);

      pepSearchFile = PepSearchFile.filter(pepSearchFile,
          opts.mLo, opts.mHi, opts.excludeMzRanges, opts.decoyTreatment, opts.predicates);

      int spmsAfterFiltering = pepSearchFile.pepSearchResults.stream()
          .mapToInt(pepSearchResult -> pepSearchResult.spms.size()).sum();
      totalSpmsAfterFiltering.addAndGet(spmsAfterFiltering);


      // done reading/processing input file
      pepSearchFiles.put(raw, pepSearchFile);
    }


    if (!couldNotReadFiles.isEmpty())
      throw new IllegalStateException("Not all input files could be read.");


    log.info("Loaded {} files total, containing: {} datasets, {} PSMs total, "
            + "{} PSMs left after filtering.",
        pepSearchFiles.size(), pepSearchFiles.values().stream()
            .mapToInt(psf -> psf.pepSearchResults.size()).sum(),
        totalSpmsBeforeFiltering.get(), totalSpmsAfterFiltering.get());


    return new ArrayList<>(pepSearchFiles.values());
  }

  private List<String> filterOptsDescription(UserOptsInputFiles opts) {
    List<String> desciptors = new ArrayList<>();

    // global delta mass range
    final boolean isLoGood = NumUtils.isGoodDouble(opts.mLo);
    final boolean isHiGood = NumUtils.isGoodDouble(opts.mHi);
    if (isLoGood || isHiGood) {
      StringBuilder sb = new StringBuilder();
      sb.append("Corrected delta mass: [")
        .append(isLoGood ? String.format("%.1f", opts.mLo) : "inf").append("; ")
        .append(isLoGood ? String.format("%.1f", opts.mHi) : "inf");
      desciptors.add(sb.toString());
    }

    // delta mass exclusions
    for (int i = 0; i < opts.excludeMzRanges.size(); i += 2) {
      Double lo = opts.excludeMzRanges.get(i);
      Double hi = opts.excludeMzRanges.get(i + 1);
      if (isGoodDouble(lo) && isGoodDouble(hi)) {
        desciptors.add(String.format("Excluding delta mass range: [%.1f; %.1f]", lo, hi));
      } else {
        throw new IllegalStateException("Some values in excludes list were not finite doubles.");
      }
    }

    // When DecoyTreatment is USE_BOTH no predicates are added
    if (DecoyTreatment.FORWARDS_ONLY.equals(opts.decoyTreatment)) {
      desciptors.add("Leaving only: Non-decoy PSMs");
    } else if (DecoyTreatment.DECOYS_ONLY.equals(opts.decoyTreatment)) {
      desciptors.add("Leaving only: Decoy PSMs");
    }

    if (opts.predicates != null) {
      opts.predicates.stream().map(SpmPredicateFactory::toString).forEach(desciptors::add);
    }

    return desciptors;
  }

  /**
   *
   * @param inputPaths Can be directories.
   * @param regex To be matched against the whole path.
   * @return List of file paths that match.
   */
  private List<Path> inputFiles(List<Path> inputPaths, final Pattern regex, boolean recursive) {
    List<Path> matched = new ArrayList<>();

    if (regex != null) {
      // match by regex
      for (Path path : inputPaths) {
        if (!Files.isDirectory(path)) {
          matched.add(path);
        } else {
          matched.addAll(
              new FileListing(path, regex).setRecursive(recursive).setIncludeDirectories(false)
                  .findFiles());
        }
      }
    } else {
      // all supported

    }
    return matched;
  }

  private List<Path> collectSupportedFiles(Iterable<? extends Path> paths, boolean recursive) {
    final ConcurrentLinkedQueue<Path> supported = new ConcurrentLinkedQueue<>();

    for (Path path : paths) {
      if (Thread.interrupted()) {
        throw new RuntimeException("Interrupted");
      }
      if (Files.isDirectory(path)) {
        try {
          final int depth = recursive ? Integer.MAX_VALUE : 1;
          final EnumSet<FileVisitOption> fileVisitOptions = EnumSet.noneOf(FileVisitOption.class);
          final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (!Files.isReadable(dir))
                return FileVisitResult.SKIP_SUBTREE;
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              IParserFactory<IPepidParser> parser = PepidParserRegistry.find(file);
              if (parser != null) {
                supported.add(file);
              }
              return FileVisitResult.CONTINUE;
            }
          };
          Files.walkFileTree(path, fileVisitOptions, depth, visitor);
        } catch (IOException e) {
          log.warn("Error collecting files for deletion in subtree: {}" + path, e);
        }
      } else {
        if (CacheLocator.isPepCacheFile(path))
          supported.add(path);
      }
    }
    return new ArrayList<>(supported);
  }

  /**
   * Find files that are supported by installed parsers.
   * @param files List of files to check against installed parsers.
   * @return A list that should not be modified.
   */
  private List<Path> supportedFiles(List<Path> files) {
    return files.stream()
        .filter(path -> PepidParserRegistry.find(path) != null)
        .collect(Collectors.toList());
  }

  /**
   * Find the cache files.
   * @param files List of input files to check.
   * @return Mapping from input paths to corresponding cache paths.
   */
  private Map<Path, Path> cachedFiles(List<Path> files) {
    Map<Path, Path> cached = new HashMap<>();
    for (Path inputFile : files) {
      Path cachedPath = CacheLocator.locateForPep(inputFile);
      if (Files.exists(cachedPath)) {
        cached.put(inputFile, cachedPath);
      }
    }
    return cached;
  }

  private PepSearchFile readRaw(Path path, FileReadingOpts opts) throws IOException {
    if (Thread.interrupted()) {
      throw new RuntimeException("Interrupted");
    }
    IParserFactory<IPepidParser> factory = PepidParserRegistry.find(path);
    if (factory == null) throw new IllegalStateException("Couldn't find parser factory.");
    PepSearchFile psf = factory.create(path).parse(opts.decoyRegex);
    // when reading data we always need it in sorted order by mass diff
    psf.pepSearchResults.forEach(psr -> psr.sort(SortOrder.DELTA_MASS_ASCENDING));
    return psf;
  }

  private PepSearchFile readCached(Path path) throws IOException {
    if (Thread.interrupted()) {
      throw new RuntimeException("Interrupted");
    }
    try {
      return DataStoreUtils.deserialize(path, PepSearchFile.class);
    } catch (ParsingException e) {
      throw new IOException(e);
    }
  }

  private void writeCached(Path path, PepSearchFile pepSearchFile) throws IOException {
    try {
      DataStoreUtils.serialize(path, pepSearchFile);
    } catch (ParsingException e) {
      throw new IOException(e);
    }
  }

  /**
   *
   * @param psf
   * @param opts
   * @return True if input {@code PepSearchFile} was modified, thus might need to be
   *    serialized again.
   * @throws IOException When can't read original raw files in case of PEP_ID mass correction.
   */
  private boolean updateMassCorrection(PepSearchFile psf, MassCorrectionOpts opts) throws IOException {
    boolean updated = false;

    for (PepSearchResult pepSearchResult : psf.pepSearchResults) {
      if (Thread.interrupted()) {
        throw new RuntimeException("Interrupted");
      }
      if (pepSearchResult.massCorrection.equals(opts.massCorrection))
        continue;
      MassCorrector mc;
      switch (opts.massCorrection) {
        case NONE:
          mc = new MassCorrectorNone();
          break;

        case ZERO_PEAK:
          mc = new MassCorrectorZeroPeak();
          break;


        case PEP_ID:
          mc = new MassCorrectionMs1(opts.scoreNameForPsmSorting, opts.additionalSearchPaths, opts.noCache);
          break;

//        case MS1_TRACE:
//          throw new NotImplementedException("TODO: Just trace MS1 without pep ids"); // TODO: Not implemented

        default:
          throw new IllegalStateException("Unknown enum element for MassCorrection");
      }
      mc.apply(pepSearchResult);
      updated = true;
    }

    return updated;
  }

  public static class FoundFiles {
    final public List<Path> input;
    final public  List<Path> supported;
    final public Map<Path, Path> cached;

    public FoundFiles(List<Path> input,
        List<Path> supported,
        Map<Path, Path> cached) {
      this.input = input;
      this.supported = supported;
      this.cached = cached;
    }
  }

  private static class MassCorrectionOpts {
    final MassCorrection massCorrection;
    final List<Path> additionalSearchPaths;
    final String scoreNameForPsmSorting;
    final boolean noCache;

    private MassCorrectionOpts(MassCorrection massCorrection,
        List<Path> additionalSearchPaths, String scoreNameForPsmSorting, boolean noCache) {
      this.massCorrection = massCorrection;
      this.additionalSearchPaths = additionalSearchPaths;
      this.scoreNameForPsmSorting = scoreNameForPsmSorting;
      this.noCache = noCache;
    }
  }

  private static class FileReadingOpts {
    final Pattern decoyRegex;

    public FileReadingOpts(Pattern decoyRegex) {
      this.decoyRegex = decoyRegex;
    }
  }
}
