package com.dmtavt.deltamass.data;

import static com.dmtavt.deltamass.utils.NumUtils.isGoodDouble;

import com.dmtavt.deltamass.args.DecoyTreatment;
import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.predicates.ISpmPredicate;
import com.dmtavt.deltamass.predicates.SpmPredicateFactory;
import com.dmtavt.deltamass.predicates.SpmPredicateFactory.ScoreNameNotExists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PepSearchResult implements Serializable {

  private static final long serialVersionUID = 8810968807763917692L;
  private static final Logger log = LoggerFactory.getLogger(PepSearchResult.class);
  /**
   * Raw file name, the original LCMS file from which the search results file was derived. E.g.
   * `thermo-01.RAW`
   */
  public final String rawFileName;
  /**
   * Search results file name. E.g. `interact-123.pepxml`.
   */
  public final String sourceFileName;
  /**
   * Directory where the search results file was located.
   */
  public final String sourceFileDir;

  public final ArrayList<Spm> spms;
  private SortOrder sortOrder;
  /**
   * Individual SPMs reference proteins by index in this list.
   */
  public final ArrayList<String> proteinAccessions;
  public final HashMap<String, Integer> scoreMapping;
  public MassCorrection massCorrection;

  public PepSearchResult(String rawFileName, String sourceFileName, String sourceFileDir,
      ArrayList<Spm> spms, ArrayList<String> proteinAccessions,
      HashMap<String, Integer> scoreMapping,
      MassCorrection massCorrection) {
    this.rawFileName = rawFileName;
    this.sourceFileName = sourceFileName;
    this.sourceFileDir = sourceFileDir;
    this.spms = spms;
    this.proteinAccessions = proteinAccessions;
    this.scoreMapping = scoreMapping;
    this.massCorrection = massCorrection;
    this.sortOrder = SortOrder.NONE;
  }

  public enum SortOrder {
    NONE, DELTA_MASS_ASCENDING
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void sort(SortOrder order) {
    switch (order) {
      case NONE:
        sortOrder = SortOrder.NONE;
        log.info("Sorted {} by {}", sourceFileName, sortOrder);
        break;

      case DELTA_MASS_ASCENDING:
        spms.sort((o1, o2) -> Double.compare(o1.mDiff, o2.mDiff));
        sortOrder = SortOrder.DELTA_MASS_ASCENDING;
        log.info("Sorted {} by {}", sourceFileName, sortOrder);
        break;

      default:
        throw new UnsupportedOperationException("Unknown switch option");
    }
  }

  /**
   * Filter search results. The original SPMs will be modified, instead of fully copied. However a
   * new instance of {@link PepSearchResult} object is returned.
   *
   * @param dmLo Low delta mass cutoff.
   * @param dmHi High delta mass cutoff.
   * @param excludes Excluded delta mass ranges. Size must be a multiple of 2.
   * @param decoyTreatment What to do with decoys.
   * @param spfs Other predicates for filtering SPMs.
   * @return A new instance of {@link PepSearchResult} object with SPMs and corresponding Protein
   * Accessions lists filterd.
   */
  public static PepSearchResult filter(PepSearchResult psr,
      Double dmLo, Double dmHi, List<Double> excludes, DecoyTreatment decoyTreatment,
      List<SpmPredicateFactory> spfs) {
    if (excludes.size() % 2 != 0) {
      throw new IllegalArgumentException("Excludes' list size must be a multiple of 2.");
    }
    List<ISpmPredicate> predicates = new ArrayList<>();

    // global delta mass range
    if (isGoodDouble(dmLo)) {
      predicates.add(spm -> spm.mDiffCorrected >= dmLo);
    }
    if (isGoodDouble(dmHi)) {
      predicates.add(spm -> spm.mDiffCorrected <= dmHi);
    }

    // delta mass exclusions
    for (int i = 0; i < excludes.size(); i += 2) {
      Double lo = excludes.get(i);
      Double hi = excludes.get(i + 1);
      if (isGoodDouble(lo) && isGoodDouble(hi)) {
        predicates.add(spm -> spm.mDiffCorrected <= lo || spm.mDiffCorrected >= hi);
      } else {
        throw new IllegalStateException("Some values in excludes list were not finite doubles.");
      }
    }

    // When DecoyTreatment is USE_BOTH no predicates are added
    if (DecoyTreatment.FORWARDS_ONLY.equals(decoyTreatment)) {
      predicates.add(spm -> !spm.isDecoy);
    } else if (DecoyTreatment.DECOYS_ONLY.equals(decoyTreatment)) {
      predicates.add(spm -> spm.isDecoy);
    }

    if (spfs != null) {
      for (SpmPredicateFactory spf : spfs) {
        try {
          ISpmPredicate predicate = spf.create(psr.scoreMapping);
          predicates.add(predicate);
        } catch (ScoreNameNotExists snne) {
          throw new IllegalStateException(snne);
        }
      }
    }

    if (predicates.isEmpty()) {
      return psr;
    }

    // compose all predicates into one
    Predicate<Spm> composite = predicates.get(0);
    if (predicates.size() > 1) {
      for (int i = 1; i < predicates.size(); i++) {
        composite = composite.and(predicates.get(i));
      }
    }

    // perform actual filtering
    ArrayList<Spm> spmsFiltered = psr.spms.stream().filter(composite)
        .collect(Collectors.toCollection(ArrayList::new));
    spmsFiltered.trimToSize();

    // weed out prot ids that are no longer in use after filtering spm list
    ArrayList<String> protAccessionsFiltered = new ArrayList<>();
    int[] protIdRemap = new int[psr.proteinAccessions.size()];
    Arrays.fill(protIdRemap, -1);
    int ptr = -1;
    for (Spm spm : spmsFiltered) {

      // main prot id
      if (protIdRemap[spm.protId] < 0) {
        protIdRemap[spm.protId] = ++ptr;
        protAccessionsFiltered.add(psr.proteinAccessions.get(spm.protId));
      }
      spm.protId = protIdRemap[spm.protId];

      // alt prot ids
      int[] protIdAlt = spm.protIdAlt;
      for (int i = 0; i < protIdAlt.length; i++) {
        int altProdIdx = protIdAlt[i];
        if (protIdRemap[altProdIdx] < 0) {
          protIdRemap[altProdIdx] = ++ptr;
          protAccessionsFiltered.add(psr.proteinAccessions.get(altProdIdx));
        }
        protIdAlt[i] = protIdRemap[altProdIdx];
      }
    }
    protAccessionsFiltered.trimToSize();

    return new PepSearchResult(psr.rawFileName, psr.sourceFileName,
        psr.sourceFileDir,
        spmsFiltered, protAccessionsFiltered, psr.scoreMapping, psr.massCorrection);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PepSearchResult that = (PepSearchResult) o;

    if (rawFileName != null ? !rawFileName.equals(that.rawFileName) : that.rawFileName != null) {
      return false;
    }
    if (sourceFileName != null ? !sourceFileName.equals(that.sourceFileName)
        : that.sourceFileName != null) {
      return false;
    }
    return sourceFileDir != null ? sourceFileDir.equals(that.sourceFileDir)
        : that.sourceFileDir == null;
  }

  @Override
  public int hashCode() {
    int result = rawFileName != null ? rawFileName.hashCode() : 0;
    result = 31 * result + (sourceFileName != null ? sourceFileName.hashCode() : 0);
    result = 31 * result + (sourceFileDir != null ? sourceFileDir.hashCode() : 0);
    return result;
  }
}
