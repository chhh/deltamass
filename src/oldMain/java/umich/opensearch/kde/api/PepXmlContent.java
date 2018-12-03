/*
 * Copyright (c) 2016 Dmitry Avtonomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package umich.opensearch.kde.api;

import static java.lang.System.out;

import com.github.chhh.utils.FileUtils;
import com.github.chhh.utils.LogUtils;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.exceptions.ParsingException;
import com.github.chhh.utils.files.FileSizeUnit;
import com.github.chhh.utils.ser.DataStoreUtils;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.pepxml.PepXmlParser;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.MsmsPipelineAnalysis;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.MsmsRunSummary;
import umich.opensearch.kde.KDEMain;
import umich.opensearch.kde.OpenSearchParams;
import umich.opensearch.kde.params.MassCorrection;
import umich.opensearch.kde.params.predicates.IsDecoyPredicate;
import umich.opensearch.kde.params.predicates.IsForwardPredicate;
import umich.opensearch.kde.params.predicates.NamedPredicate;
import umich.opensearch.kde.params.predicates.ScorePredicate;
import umich.opensearch.kde.pepxml.DecoyDetector;
import umich.opensearch.kde.pepxml.MsmsRunSummaryUtils;
import umich.opensearch.kde.util.SimpleConcurrentList;

/**
 * A storage for the contents of a single .pep.xml file.
 *
 * @author Dmitry Avtonomov
 */
public class PepXmlContent implements Serializable {

  private static final long serialVersionUID = 2912576418487478090L;

  private static final Logger log = LoggerFactory.getLogger(PepXmlContent.class);
  private static final boolean DEBUG = false;
  private static final String TAG_RUN_SUMMARY = "msms_run_summary";
  private List<SearchHitDiffs> hitsList;
  private URI uri;

  public PepXmlContent(URI uri) {
    if (uri == null) {
      throw new IllegalArgumentException("URI must be non null");
    }
    this.hitsList = new ArrayList<>();
    this.uri = uri;
  }

  public PepXmlContent(List<SearchHitDiffs> hitsList) {
    this.hitsList = hitsList;
  }

  public static List<PepXmlContent> getPepXmlContents(final OpenSearchParams params,
      List<Path> matchingPaths) {
    final ArrayList<PepXmlContent> pepXmlList = new SimpleConcurrentList<>();

    long timeLo = System.nanoTime();

    int numCores = Runtime.getRuntime().availableProcessors();
    int numThreadsToUse = numCores;
    if (params.getThreads() > 0) {
      numThreadsToUse = Math.min(numCores, params.getThreads());
    }
    ExecutorService exec = Executors.newFixedThreadPool(numThreadsToUse);

    final int memPerThreadMb = 32;
    final int bufSize = memPerThreadMb * 1024 * 1024;

    for (final Path path : matchingPaths) {
      exec.execute(() -> parseSingleFile(params, pepXmlList, path, bufSize));
    }

    exec.shutdown();
    try {
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      log.error("Something awful happened, could not stop Executor Service");
      System.exit(1);
    }

    long timeHi = System.nanoTime();

    return pepXmlList;
  }

  /**
   * Can be called asynchhronously, so please synchronize access to 'toAddTo' list.
   *
   * @param toAddTo Synchronize access to this list. That's where all the results are stored.
   * @param pepXmlPath Path to a pep.xml file to be parsed.
   * @param bufSize Buffer size used for reading pep.xml. Use 8192 if unsure.
   */
  public static void parseSingleFile(OpenSearchParams params,
      final ArrayList<PepXmlContent> toAddTo, Path pepXmlPath, int bufSize) {
    if (Thread.interrupted()) {
      return;
    }
    try {
      // if caching is on and we find a cached file - just read it
      PepXmlContent pepXmlContent = null;
      boolean doesNeedSerialization = false;
      MassCorrection massCorrection = params.getMassCorrection();
      if (params.isUseCache()) {
        Path pathToCached = checkCacheExists(pepXmlPath, params);
        if (pathToCached != null) {
          // deserialize
          try {
            log.info(
                String.format("Cached entry '%s' was found for file '%s', trying to deserialize.",
                    pathToCached.toString(), pepXmlPath.toString()));
            pepXmlContent = parseCached(pathToCached);
            log.debug("Done deserializing '{}'", pathToCached);

            // check that serialized data had the right mass correction
            boolean isRightMassCorrection = true;
            if (massCorrection != MassCorrection.NONE) {
              for (SearchHitDiffs searchHitDiffs : pepXmlContent.hitsList) {
                if (searchHitDiffs.getMassCorrection() != massCorrection) {
                  isRightMassCorrection = false;
                  break;
                }
              }
            }

            if (isRightMassCorrection) {
              doesNeedSerialization = false; // all fine, no need to rewrite the data to disk
            } else {
              // fix the correction in the cache
              log.info(String.format("Cached file '%s' had wrong mass correction, fixing",
                  pathToCached.toString()));
              switch (massCorrection) {
                case NONE: // unreachable
                  break;
                case ZERO_PEAK:
                  for (SearchHitDiffs searchHitDiffs : pepXmlContent.hitsList) {
                    MassCorrection.correctMasses(searchHitDiffs, massCorrection, null);
                  }
                  break;
                case CAL_FILES:
                  HashMap<String, Double> massCal = parseMassCorrectionFile(pepXmlPath.toUri(),
                      params);
                  for (SearchHitDiffs searchHitDiffs : pepXmlContent.hitsList) {
                    MassCorrection.correctMasses(searchHitDiffs, massCorrection, massCal);
                  }
                  break;
              }
              doesNeedSerialization = true; // setting this to false will trigger re-saving the data to disk
            }

          } catch (Exception e) {
            // we couldn't deserialize, that's not the end of the world, we can still try re-parse
            log.error(String.format(
                "Could not deserialize cached file '%s', falling back to reading the original.",
                pathToCached));
          }
        }
      }

      // parse from file and save (apply mass correction before removing entries)
      if (pepXmlContent == null) {
        doesNeedSerialization = true;
        log.info(String.format("Unmarshalling '%s' file '%s' (%.2fMB)",
            MsmsPipelineAnalysis.class.getSimpleName(),
            pepXmlPath.toString(), FileUtils.fileSize(pepXmlPath, FileSizeUnit.MB)));

        try (BufferedInputStream bis = new BufferedInputStream(
            new FileInputStream(pepXmlPath.toFile()), bufSize)) {
          pepXmlContent = parse(pepXmlPath.toUri(), bis, params);
          if (pepXmlContent == null) {
            throw new IOException("Parsing returned null");
          }

          // we might need to do mass correction
          if (massCorrection != MassCorrection.NONE) {
            log.info(String
                .format("Performing mass correction for file '%s'", pepXmlContent.uri.toString()));
            switch (massCorrection) {
              case NONE: // unreachable
                break;
              case ZERO_PEAK:
                for (SearchHitDiffs searchHitDiffs : pepXmlContent.hitsList) {
                  MassCorrection.correctMasses(searchHitDiffs, massCorrection, null);
                }
                break;
              case CAL_FILES:
                HashMap<String, Double> massCal = parseMassCorrectionFile(pepXmlPath.toUri(),
                    params);
                for (SearchHitDiffs searchHitDiffs : pepXmlContent.hitsList) {
                  MassCorrection.correctMasses(searchHitDiffs, massCorrection, massCal);
                }
                break;
            }
            log.debug("Done running mass correction for '{}'", pepXmlContent.uri.toString());
          }
        } catch (IOException e) {
          log.error(String
                  .format("Error unmarshalling file '%s'.\n%s", pepXmlPath.toString(), e.getMessage()),
              e);
        }


      }

      if (pepXmlContent == null) {
        // we couldn't parse or deserialize the file, just move on to the next one
        String msg = String
            .format("Could not parse or deserialize '%s', skipping", pepXmlPath.toString());
        log.error(msg);
        LogUtils.println(out, msg);
        return;
      }
      synchronized (PepXmlContent.class) {
        toAddTo.add(pepXmlContent);
      }

      log.debug("Done unmarshalling '{}' from file '{}'",
          MsmsPipelineAnalysis.class.getSimpleName(), pepXmlPath.toString());

      if (params.isUseCache() && doesNeedSerialization) {
        // delete a previously saved file
        // this is only hit if we had problems deserializing, and proceeded to just reparsing the file
        Path pathToCachedFile = checkCacheExists(pepXmlPath, params);
        if (pathToCachedFile != null) {
          log.info("Deleting old cache file (" + pathToCachedFile.toString() + ")");
          try {
            Files.delete(pathToCachedFile);
          } catch (IOException e) {
            String msg = String
                .format("Could not delete cached file '%s', reason: %s", pepXmlPath.toString(),
                    e.getMessage());
            log.error(msg, e);
            LogUtils.println(out, msg);
          }
        }
        // and store the new cached version
        Path cachedPath = createCachedPath(pepXmlPath, params);
        log.info("Writing new cache file (" + cachedPath.toString() + ")");
        DataStoreUtils.serialize(cachedPath, pepXmlContent);
      }

      // remove entries that we are not interested in
      pepXmlContent.filter(params);

    } catch (ParsingException e) {
      log.error("Could not parse file '%s'", pepXmlPath.toString(), e);
    }
  }

  public static void deleteCache(OpenSearchParams params) {
    List<Path> matchingPaths = KDEMain.findMatchingPaths(params);
    log.info(String.format("Found %d files matching input (single files and '%s' pattern) in '%s'",
        matchingPaths.size(), params.getInFileRegex(),
        StringUtils.join(params.getInFilePath(), ", ")));
    for (Path p : matchingPaths) {
      Path cachedPath = checkCacheExists(p, params);
      if (cachedPath != null) {
        try {
          log.info(
              String.format("Found cached file '%s', will delete now.", cachedPath.toString()));
          Files.delete(cachedPath);
        } catch (IOException e) {
          log.error("Could not delete cached file", e);
        }
      }
    }
  }

  /**
   * Checks if there is a cached file for the original pep.xml.
   *
   * @param pathOrig path to the original file to be checked for cache existence
   * @return null if a cached file does not exist
   */
  public static Path checkCacheExists(Path pathOrig, OpenSearchParams params) {
    Path pathToCached = createCachedPath(pathOrig, params);
    if (Files.exists(pathToCached, params.getFollowSymlinkOptions())) {
      return pathToCached;
    }
    return null;
  }

  public static PepXmlContent parseCached(Path pathToCached) throws ParsingException {
    PepXmlContent pepXmlContent = DataStoreUtils.deserialize(pathToCached, PepXmlContent.class);
    pepXmlContent.sortByMassDiff();
    return pepXmlContent;
  }

  public static Path createCachedPath(Path pathOrig, OpenSearchParams params) {
    return Paths.get(pathOrig.toString() + params.getCacheExt());
  }

  /**
   * Parse the diffs from a stream.
   *
   * @param uri URI of the source of the input stream, e.g. a file.
   * @param is the stream to parse XML from. If you want buffering, wrap it into a buffered stream
   * yourself.
   * @return null if thread was interrupted or
   */
  public static PepXmlContent parse(URI uri, InputStream is, OpenSearchParams params)
      throws ParsingException {
    if (Thread.interrupted()) {
      return null;
    }
    PepXmlContent pepXmlContent = new PepXmlContent(uri);

    try (BufferedInputStream bis = new BufferedInputStream(is)) {
      // we'll manually iterate over msmsRunSummaries - won't need so much memory at once for processing large files.
      Iterator<MsmsRunSummary> pepIt = PepXmlParser.parse(bis);
      while (pepIt.hasNext()) {
        long timeLo = System.nanoTime();
        MsmsRunSummary runSummary = pepIt.next();
        long timeHi = System.nanoTime();
        if (DEBUG) {
          System.err.printf("Unmarshalling took %.4fms (%.2fs)\n", (timeHi - timeLo) / 1e6,
              (timeHi - timeLo) / 1e9);
        }

        if (runSummary.getSpectrumQuery().isEmpty()) {
          log.warn(String.format(
              "Parsed msms_run_summary was empty for file '%s', summary base_name '%s'",
              uri.toString(), runSummary.getBaseName()));
        }
        DecoyDetector decoyDetector = params.getDecoyDetector();
        SearchHitDiffs searchHitDiffs = MsmsRunSummaryUtils
            .convert(runSummary, decoyDetector, null);
        pepXmlContent.getHitsList().add(searchHitDiffs);
      }
      return pepXmlContent;

    } catch (IOException | FileParsingException e) {
      throw new ParsingException(e);
    }
  }

  /**
   * Parse mass calibration from .masscor files.
   *
   * @param uri The URI of the pepxml file.
   * @return Mapping from spectral identifiers to masses.
   */
  public static HashMap<String, Double> parseMassCorrectionFile(URI uri, OpenSearchParams params)
      throws ParsingException {
    HashMap<String, Double> massCal = null;
    if (params.getMassCorrection() == MassCorrection.CAL_FILES) {
      Path pathPepXml = Paths.get(uri);

      List<Path> possiblePaths = new ArrayList<>();

      Path pathCalFile = Paths.get(pathPepXml.toString() + ".masscorr");

      if (!params.getMassCorFilePaths().isEmpty()) {
        Path fileName = pathCalFile.getFileName();
        for (Path p : params.getMassCorFilePaths()) {
          if (!Files.isDirectory(p)) {
            continue;
          }
          Path possiblePath = p.resolve(fileName);
          if (Files.exists(possiblePath)) {
            possiblePaths.add(possiblePath);
          }
        }
      } else {
        if (Files.exists(pathCalFile)) {
          possiblePaths.add(pathCalFile);
        }
      }

      if (possiblePaths.isEmpty()) {
        log.info("Mass correction file for pepxml not found (" + uri + ")");
        return null;
      }

      log.info(String.format("Found mass correction file: %s", pathCalFile));
      File fileCal = possiblePaths.get(0).toFile(); // take the first one
      massCal = new HashMap<>(10000);
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(new FileInputStream(fileCal), Charset.forName("UTF-8")))) {
        String line;
        String[] split;
        Pattern splitRegex = Pattern.compile("\\s+");
        double massDiffCorrected;
        String spectrumQueryString;
        while ((line = br.readLine()) != null) {
          split = splitRegex.split(line);
          spectrumQueryString = split[0];
          massDiffCorrected = Double.parseDouble(split[2]);
          massCal.put(spectrumQueryString, massDiffCorrected);
        }
      } catch (IOException e) {
        throw new ParsingException(
            "Found matching calibration file, but something went wrong while reading it", e);
      }

    }
    return massCal;
  }

  private static boolean advanceReaderToNextRunSummary(XMLStreamReader xsr)
      throws XMLStreamException {
    long timeLo = System.nanoTime();
    do {
      if (xsr.next() == XMLStreamConstants.END_DOCUMENT) {
        return false;
      }
    } while (!(xsr.isStartElement() && xsr.getLocalName().equals(TAG_RUN_SUMMARY)));

    long timeHi = System.nanoTime();
    if (DEBUG) {
      System.err.printf("Advancing reader took: %.4fms\n", (timeHi - timeLo) / 1e6d);
    }

    return true;
  }

  public URI getUri() {
    return uri;
  }

  public List<SearchHitDiffs> getHitsList() {
    return hitsList;
  }

  public int size() {
    int size = 0;
    for (SearchHitDiffs searchHitDiffs : hitsList) {
      size += searchHitDiffs.getHits().size();
    }
    return size;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public void sortByMassDiff() {
    for (SearchHitDiffs searchHitDiffs : hitsList) {
      if (!searchHitDiffs.isSorted()) {
        searchHitDiffs.sortByDiffMass();
      }
    }
  }

  /**
   * Remove everything that doesn't meet the criteria from params.
   */
  public int filter(OpenSearchParams params) {
    NamedPredicate<SearchHitDiff> predicate;
    int totalRemoved = 0;
    boolean didFiltering = false;
    for (SearchHitDiffs searchHitDiffs : hitsList) {
      log.debug("Filtering started.");
      List<NamedPredicate<SearchHitDiff>> predicates = new ArrayList<>(10);
      switch (params.getDecoysTreatment()) {
        case FORWARDS_ONLY:
          log.debug("Creating IsForwardPredicate");
          predicate = new IsForwardPredicate(); // leave forwards
          predicates.add(predicate);
          break;
        case DECOYS_ONLY:
          log.debug("Creating IsDecoyPredicate");
          predicate = new IsDecoyPredicate(); // leave decoys
          predicates.add(predicate);
          break;
        case USE_BOTH:
          // not removing neither decoys nor forwards, don't need to add any predicates
          break;
        default:
          throw new IllegalStateException(
              "Unknown enum value encountered for params.getDecoysTreatment()");
      }
      predicate = params.getPredicateMassRange();
      if (predicate != null) {
        log.debug("Creating MassRange predicate");
        predicates.add(predicate);
      }
      List<ScorePredicate.Factory> factories = params.getScorePredicateFactories();
      if (factories != null && !factories.isEmpty()) {
        log.debug("Found {} score factories, creating predicates for them", factories.size());
        for (ScorePredicate.Factory factory : factories) {
          log.debug("Creating predicate '{}'", factory.toString());
          try {
            predicate = factory.create(searchHitDiffs.getMapScoreName2Index());
            predicates.add(predicate);
          } catch (ParsingException e) {
            String msg = "Could not configure score predicate, probably the file didn't have that score in the first place, skipping that filter.";
            log.error(msg, e);
            LogUtils.println(out, msg + "\n" + e.getMessage());
            throw new RuntimeException(e);
          }
        }
      }
      log.debug("Done composing predicates for filtering, total {} predicates.", predicates.size());
      if (!predicates.isEmpty()) {
        didFiltering = true;
        log.info("Filtering MsMsRun '{}' from file '{}'", searchHitDiffs.getName(), uri.toString());
        for (NamedPredicate<SearchHitDiff> p : predicates) {
          log.info("\tLeaving sequences matching: {}", p.getDescription());
        }
        Predicate<SearchHitDiff> criteria =
            predicates.size() == 1 ? predicates.get(0) : Predicates.and(predicates);
        int filtered = searchHitDiffs.filter(criteria);
        totalRemoved += filtered;

        log.info("\tRemoved {} entries, {} entries remaining", filtered,
            searchHitDiffs.getHits().size());
      }
      log.debug("Filtering done.");
    }

    if (didFiltering) {
      int remainingEntries = 0;
      for (SearchHitDiffs searchHitDiffs : hitsList) {
        remainingEntries += searchHitDiffs.getHits().size();
      }
      log.info("Overall removed {} entries, {} entries remaining", totalRemoved, remainingEntries);
    }

    return totalRemoved;
  }
}
