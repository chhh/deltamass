package umich.opensearch.kde.pepxml;

import com.github.chhh.utils.MathUtils;
import com.google.common.primitives.Doubles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.opensearch.kde.api.SearchHitDiff;
import umich.opensearch.kde.api.SearchHitDiffs;
import umich.opensearch.kde.params.MassCorrection;
import umich.ptm.chem.Table;

/**
 * @author Dmitry Avtonomov
 */
public class MsmsRunSummaryUtils {

  private static final Logger log = LoggerFactory.getLogger(MsmsRunSummaryUtils.class);
  private static final String PEP_PROPH = "peptideprophet";
  private static final String I_PROPH = "interprophet";

  private MsmsRunSummaryUtils() {
  }

  public static SearchHitDiffs convert(MsmsRunSummary summary, DecoyDetector detector,
      HashMap<String, Double> massCal) {
    SearchHitCount count = countSearchHits(summary, detector);
    ArrayList<SearchHitDiff> result = new ArrayList<>(count.getTotal());

    double precursorNeutralMass, massDiff, calcNeutralPepMass, massDiffCalced;
    int assumedCharge;
    double observedMzInferred;
    final double PROTON_MASS = Table.Proton.monoMass;
    Double rt;
    Long startScan;
    int[] chargeStatesToCheck = {1, 2, 3, 4, 5, 6, 7};
    int hitsWithPossibleWrongCharge = 0;
    double mzToCheck;
    final double PPM_PLUS_MINUS = 20;
    int pepProphIdx = -1; // less than zero means it's not there
    int iProphIdx = -1; // less than zero means it's not there

    int hitsTotal = 0;
    int hitsModified = 0;
    int hitsDecoy = 0;

    List<SearchResult> searchResults;
    List<SpectrumQuery> spectrumQueries;
    List<SearchHit> searchHits;
    SearchHit searchHitFirst = null;

    log.info(String.format("Processing msms_run_summary '%s'", summary.getBaseName()));

    Map<String, Integer> mapScore2Index = new HashMap<>(10);

    spectrumQueries = summary.getSpectrumQuery();
    for (SpectrumQuery spectrumQuery : spectrumQueries) {

      precursorNeutralMass = spectrumQuery.getPrecursorNeutralMass();
      rt = spectrumQuery.getRetentionTimeSec();
      startScan = spectrumQuery.getStartScan();
      assumedCharge = spectrumQuery.getAssumedCharge();
      observedMzInferred = (precursorNeutralMass + assumedCharge * PROTON_MASS) / assumedCharge;

      searchResults = spectrumQuery.getSearchResult();
      for (SearchResult searchResult : searchResults) {

        searchHits = searchResult.getSearchHit();
        int searchHitNumInQuery = 0;
        for (SearchHit searchHit : searchHits) {
          if (searchHitFirst == null) {
            // this is the first hit, we'll use it to determine the size of score arrays,
            // including peptide prophet and iprophet analysis
            searchHitFirst = searchHit;

            // get regular scores
            List<NameValueType> searchScore = searchHitFirst.getSearchScore();
            int scoreIndex = 0;
            for (; scoreIndex < searchScore.size(); scoreIndex++) {
              NameValueType score = searchScore.get(scoreIndex);
              String scoreName = score.getName();
              mapScore2Index.put(scoreName, scoreIndex);
            }

            // try get peptide prophet score
            List<AnalysisResult> analysisResults = searchHit.getAnalysisResult();
            for (AnalysisResult analysisResult : analysisResults) {
              String analysis = analysisResult.getAnalysis();
              switch (analysis) {
                case PEP_PROPH:
                  pepProphIdx = scoreIndex;
                  mapScore2Index.put(PEP_PROPH, scoreIndex);
                  scoreIndex++;
                  break;
                case I_PROPH:
                  iProphIdx = scoreIndex;
                  mapScore2Index.put(I_PROPH, scoreIndex);
                  scoreIndex++;
                  break;
              }
            }
          }
          searchHitNumInQuery++;
          hitsTotal++;

          if (searchHit.getModificationInfo() != null) {
            hitsModified++;
          }

          massDiff = searchHit.getMassdiff();
          calcNeutralPepMass = searchHit.getCalcNeutralPepMass();
          //massDiffCalced = precursorNeutralMass - calcNeutralPepMass;
          massDiffCalced = massDiff;

          SearchHitDiff shd = new SearchHitDiff(mapScore2Index.size());
          shd.setMassDiff(massDiffCalced);
          shd.setCalcedNeutralMass(calcNeutralPepMass);
          shd.setObservedNeutralMass(precursorNeutralMass);
          shd.setCharge(assumedCharge);
          shd.setSpectrumId(spectrumQuery.getSpectrum());
          if (massCal != null) {
            Double calibrated = massCal.get(spectrumQuery.getSpectrum());
            if (calibrated == null) {
              log.warn("Could not find calibrated mass for spectrum query (" + spectrumQuery
                  .getSpectrum() + ")");
            } else {
              shd.setMassDiffCal(calibrated);
            }
          } else {
            shd.setMassDiffCal(massDiffCalced);
          }
          shd.setSeq(searchHit.getPeptide());
          if (startScan != null) {
            shd.setScanNum(startScan.intValue());
            startScan = null;
          }
          if (rt != null) {
            shd.setRtSec(rt);
            rt = null;
          }

          List<NameValueType> searchScores = searchHit.getSearchScore();
          double[] scores = shd.getScores();
          for (int i = 0; i < searchScores.size(); i++) {
            try {
              scores[i] = Doubles.tryParse(searchScores.get(i).getValueStr());
            } catch (NumberFormatException e) {
              scores[i] = Double.NaN;
              log.warn("Could not parse score #{} for spectrum query '{}' search hit '{}'", i,
                  spectrumQuery.getSpectrum(), searchHitNumInQuery);
            }
          }
          if (pepProphIdx > -1 || iProphIdx > -1) {
            List<AnalysisResult> analysisResults = searchHit.getAnalysisResult();
            for (AnalysisResult analysisResult : analysisResults) {
              final String analysis = analysisResult.getAnalysis();
              final List<Object> any = analysisResult.getAny();
              for (Object o : any) {
                if (o instanceof PeptideprophetResult) {
                  scores[pepProphIdx] = ((PeptideprophetResult) o).getProbability();
                } else if (o instanceof InterprophetResult) {
                  scores[iProphIdx] = ((InterprophetResult) o).getProbability();
                } else {
                  log.error("Unknown AnalysisResult type encountered: '{}'", analysis);
                }
              }
            }
          }

          if (detector.apply(searchHit)) {
            shd.setDecoy(true);
            hitsDecoy++;
          } else {
            shd.setDecoy(false);
          }
          if (massDiff > 2) {
            // then check if this diff can be attributed to misassigned charge
            for (int z : chargeStatesToCheck) {
              mzToCheck = (calcNeutralPepMass + z * PROTON_MASS) / z;
              if (MathUtils.isWithinPpm(observedMzInferred, mzToCheck, PPM_PLUS_MINUS)) {
                if (z != assumedCharge) {
                  hitsWithPossibleWrongCharge++;
                  break;
                }
              }
            }
          }
          result.add(shd);
        }
      }
    }

    MassCorrection mc = massCal == null ? MassCorrection.NONE : MassCorrection.CAL_FILES;
    SearchHitDiffs searchHitDiffs = new SearchHitDiffs(result, mapScore2Index, mc);

    searchHitDiffs.sortByDiffMass();
    searchHitDiffs.setName(summary.getBaseName());
    long timeSortStart = System.nanoTime();
    log.debug("Started sorting MsMsRunSummary by diff mass",
        (System.nanoTime() - timeSortStart) / 1e6d);
    searchHitDiffs.sortByDiffMass();
    log.debug(String.format("Sorting took %.3fms", (System.nanoTime() - timeSortStart) / 1e6d));
    log.info(String.format("There were a total of %d search hits in the run summary.\n" +
            "\tModified: %d\n" +
            "\tMight be incorrect charge assignments: %d\n" +
            "\tDecoy hits (%s): %d\n" +
            "\tFinal list contains: %d hits",
        hitsTotal, hitsModified, hitsWithPossibleWrongCharge,
        detector.getDescription(), hitsDecoy, result.size()));
    return searchHitDiffs;
  }

  private static SearchHitCount countSearchHits(MsmsRunSummary msmsRunSummary,
      DecoyDetector detector) {
    int hitsFwd = 0;
    int hitsRev = 0;
    List<SearchResult> searchResults;
    List<SpectrumQuery> spectrumQueries;
    List<SearchHit> searchHits;

    spectrumQueries = msmsRunSummary.getSpectrumQuery();
    for (SpectrumQuery spectrumQuery : spectrumQueries) {
      searchResults = spectrumQuery.getSearchResult();
      for (SearchResult searchResult : searchResults) {
        searchHits = searchResult.getSearchHit();
        for (SearchHit searchHit : searchHits) {
          if (detector.apply(searchHit)) {
            hitsRev++;
          } else {
            hitsFwd++;
          }
        }
      }
    }

    return new SearchHitCount(hitsFwd, hitsRev);
  }
}
