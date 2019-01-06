package com.dmtavt.deltamass.parsers;

import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.data.PepSearchFile;
import com.dmtavt.deltamass.data.PepSearchResult;
import com.dmtavt.deltamass.data.Spm;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.pepxml.PepXmlParser;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.AltProteinDataType;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.AnalysisResult;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.InterprophetResult;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.ModAminoacidMass;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.ModificationInfo;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.MsmsRunSummary;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.NameValueType;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.PeptideprophetResult;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.SearchHit;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.SearchResult;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.SearchSummary;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.SpectrumQuery;
import umich.ptm.chem.Table;

public class PepxmlParser implements IPepidParser {

  private static final Logger log = LoggerFactory.getLogger(PepxmlParser.class);
  private final Path path;
  private boolean hasNoSearchResults = false;
  private boolean hasMultipleSearchResults = false;
  private boolean hasNoSearchHits = false;
  private boolean hasMultipleSearchHits = false;
  private static final String PEPTIDE_PROPHET = "peptideprophet";
  private static final String I_PROPHET = "interprophet";
  private final double PROTON_MASS = Table.Proton.monoMass;

  public PepxmlParser(Path path) {
    this.path = path;
  }

  @Override
  public PepSearchFile parse(final Pattern decoyRegex) throws IOException {
    List<PepSearchResult> results = new ArrayList<>();

    // decoy detector
    Predicate<SearchHit> decoyDetector = searchHit -> decoyRegex.matcher(searchHit.getProtein()).find();

    try (BufferedInputStream bis = new BufferedInputStream(
        Files.newInputStream(path, StandardOpenOption.READ))) {
      Iterator<MsmsRunSummary> it = PepXmlParser.parse(bis);
      while (it.hasNext()) {
        MsmsRunSummary mmrs = it.next();
        if (mmrs.getSearchSummary().size() != 1) {
            throw new IOException(
                    "Count of SearchSummary elements in Pepxml -> MsMsRunSummary not equal " +
                            "to one. Not supported.");
        }

        SearchSummary ss = mmrs.getSearchSummary().get(0);
        Path originalRawFile;
        try {
          originalRawFile = Paths.get(mmrs.getBaseName() + mmrs.getRawData());
        } catch (Exception e) {
          throw new IOException("Could not parse MsMsRunSummary -> baseName + rawData.");
        }

        final String rawFileName = originalRawFile.getFileName().toString();
        final String sourceFileName = path.getFileName().toString();
        final String sourceFileDir = path.getParent().toString();
        final ArrayList<Spm> spms = new ArrayList<>(mmrs.getSpectrumQuery().size());
        final ArrayList<String> proteinAccessions = new ArrayList<>();

        final HashMap<String, Integer> scoreMapping = mapScoreNames(mmrs.getSpectrumQuery());
        // less than zero means peptide prophet data is not available
        int pepProphIdx = scoreMapping.getOrDefault(PEPTIDE_PROPHET, -1);
        // less than zero means iProphet data is not available
        int iProphIdx = scoreMapping.getOrDefault(I_PROPHET, -1);


        final HashMap<String, Integer> proteinMapping = new HashMap<>(); // protein Accession to Id

        PepSearchResult pepSearchResult = new PepSearchResult(
            rawFileName, sourceFileName, sourceFileDir,
            spms, proteinAccessions, scoreMapping, MassCorrection.NONE);

        for (SpectrumQuery sq : mmrs.getSpectrumQuery()) {
          final String spectrumId = sq.getSpectrum();
          final double precMassZ0 = sq.getPrecursorNeutralMass();
          final int charge = sq.getAssumedCharge();

          if (sq.getSearchResult().size() == 0 && !hasNoSearchResults) {
            hasNoSearchResults = true;
            log.warn("No SearchResult for some SearchQueries in: {}", path.toString());
            continue;
          }
          if (sq.getSearchResult().size() != 1 && !hasMultipleSearchResults) {
            hasMultipleSearchResults = true;
            log.warn("Multiple SearchResults per SearchQuery encountered. Only the first taken. "
                + "File: {}", path.toString());
          }
          SearchResult sr = sq.getSearchResult().get(0);
          if (sr.getSearchHit().size() == 0 && !hasNoSearchHits) {
            hasNoSearchHits = true;
            log.warn("No SearchHits in a SearchResult of a SpectrumQuery. File: {}", path.toString());
          }
          if (sr.getSearchHit().size() != 1 && !hasMultipleSearchHits) {
            hasMultipleSearchHits = true;
            log.warn("Multiple SearchHits per SearchResult encountered. Only the first taken. "
                + "File: {}", path.toString());
          }
          SearchHit sh = sr.getSearchHit().get(0);


          // construct an SPM
          Spm spm = new Spm(scoreMapping.size());

          final String protAccession = sh.getProtein();
          final int protIndex = proteinMapping.computeIfAbsent(protAccession, accession -> {
            proteinAccessions.add(accession);
            return proteinAccessions.size() - 1;
          });
          spm.protId = protIndex;

          List<AltProteinDataType> altProts = sh.getAlternativeProtein();
          if (altProts == null || altProts.isEmpty()) {
            spm.protIdAlt = new int[0];
          } else {
            spm.protIdAlt = new int[altProts.size()];
            for (int i = 0; i < altProts.size(); i++) {
              final String altProtAccession = altProts.get(i).getProtein();
              final int altProtIndex = proteinMapping.computeIfAbsent(altProtAccession, accession -> {
                proteinAccessions.add(altProtAccession);
                return proteinAccessions.size() - 1;
              });
              spm.protIdAlt[i] = altProtIndex;
            }
          }

          spm.mDiff = sh.getMassdiff();
          spm.mDiffCorrected = sh.getMassdiff();
          spm.mCalcNeutral = sh.getCalcNeutralPepMass();
          spm.mObsNeutral = precMassZ0;
          spm.charge = charge;
          if (charge != 0) {
            // TODO: is this value really not present in pepxml and need to be computed?
            spm.mzObs = (precMassZ0 + charge * PROTON_MASS) / charge;
          }
          spm.spectrumId = spectrumId;
          spm.seq = sh.getPeptide();
          spm.rtSec = sq.getRetentionTimeSec() == null ? Double.NaN : sq.getRetentionTimeSec();

          // decoy status
          spm.isDecoy = decoyDetector.test(sh);

          // mods string
          ModificationInfo mi = sh.getModificationInfo();
          if (mi == null) {
            spm.seqModStateId = spm.seq;
          } else {
            StringBuilder sb = new StringBuilder(mi.getModifiedPeptide());
            StringBuilder sbMods = new StringBuilder();
            if (mi.getModNtermMass() != null) {
              sb.append(String.format("_%.2f@0", mi.getModNtermMass()));
              sbMods.append(String.format("%.2f@0(Nt)", mi.getModNtermMass()));
            }
            for (ModAminoacidMass mam : mi.getModAminoacidMass()) {
              sb.append(String.format("_%.2f@%d", mam.getMass(), mam.getPosition()));
              if (sbMods.length() > 0) {
                sbMods.append(", ");
              }
              sbMods.append(String.format("%.2f@%d(%s)", mam.getMass(), mam.getPosition(), sh.getPeptide().charAt(mam.getPosition()-1)));
            }

            if (mi.getModCtermMass() != null) {
              sb.append(String.format("_%.2f@%d", mi.getModCtermMass(), sh.getPeptide().length() + 1));
              if (sbMods.length() > 0) {
                sbMods.append(", ");
              }
              sbMods.append(String.format("%.2f@%d(Ct)", mi.getModCtermMass(), sh.getPeptide().length() + 1));
            }
            spm.seqModStateId = sb.toString();
            spm.mods = sbMods.toString();
          }

          // scores
          List<NameValueType> searchScores = sh.getSearchScore();
          for (int i = 0; i < searchScores.size(); i++) {
            try {
              spm.scores[i] = Double.parseDouble(searchScores.get(i).getValueStr());
            } catch (NumberFormatException e) {
              spm.scores[i] = Double.NaN;
              throw new IllegalStateException(String.format("Could not parse score #%d for "
                  + "spectrum query `%s`", i, sq.getSpectrum()));
            }
          }
          try {
            if (pepProphIdx > -1) {
              Object analysis = sh.getAnalysisResult().stream()
                  .flatMap(analysisResult -> analysisResult.getAny().stream())
                  .filter(o -> o instanceof PeptideprophetResult).findAny()
                  .orElseThrow(() -> new IllegalStateException(String.format(
                      "PeptideProphet score could not be found in file `%s`, SpectrumQuery `%s`",
                      path, sq.getSpectrum())));
              spm.scores[pepProphIdx] = ((PeptideprophetResult) analysis).getProbability();
            }
            if (iProphIdx > -1) {
              Object analysis = sh.getAnalysisResult().stream()
                  .flatMap(analysisResult -> analysisResult.getAny().stream())
                  .filter(o -> o instanceof InterprophetResult).findAny()
                  .orElseThrow(() -> new IllegalStateException(String.format(
                      "iProphet score could not be found in file `%s`, SpectrumQuery `%s`", path,
                      sq.getSpectrum())));
              spm.scores[iProphIdx] = ((InterprophetResult) analysis).getProbability();
            }
          } catch (IllegalStateException e) {
            log.warn(e.getMessage());
            throw e;
          }
          spms.add(spm);
        }
        results.add(pepSearchResult);
      }
    } catch (FileParsingException e) {
      throw new IOException(e);
    }

    return new PepSearchFile(path.toAbsolutePath().normalize().toString(), results);
  }

  private HashMap<String, Integer> mapScoreNames(List<SpectrumQuery> queries) throws IOException {

    // find first SearchHit entry
    SearchHit sh = queries.stream()
        .flatMap(spectrumQuery -> spectrumQuery.getSearchResult().stream())
        .flatMap(searchResult -> searchResult.getSearchHit().stream())
        .findFirst().orElseThrow(() -> new IOException("No SearchHit elements found"));

    HashMap<String, Integer> scoreMapping = new HashMap<>(10);

    // get regular scores
    List<NameValueType> searchScore = sh.getSearchScore();
    int scoreIndex = 0;
    for (; scoreIndex < searchScore.size(); scoreIndex++) {
      NameValueType score = searchScore.get(scoreIndex);
      String scoreName = score.getName();
      scoreMapping.put(scoreName, scoreIndex);
    }

    // try get peptidepprophet/iprophet score
    List<AnalysisResult> analysisResults = sh.getAnalysisResult();
    for (AnalysisResult analysisResult : analysisResults) {
      String analysis = analysisResult.getAnalysis();
      switch (analysis) {
        case PEPTIDE_PROPHET:
          scoreMapping.put(PEPTIDE_PROPHET, scoreIndex++);
          break;
        case I_PROPHET:
          scoreMapping.put(I_PROPHET, scoreIndex++);
          break;
      }
    }

    return scoreMapping;
  }

  public static class Factory implements IParserFactory<IPepidParser> {

    @Override
    public boolean supports(String fileNameLowerCase, Path path) {
      return fileNameLowerCase.endsWith(".pep.xml") || fileNameLowerCase.endsWith(".pepxml");
    }

    @Override
    public IPepidParser create(Path path) {
      return new PepxmlParser(path);
    }
  }
}
