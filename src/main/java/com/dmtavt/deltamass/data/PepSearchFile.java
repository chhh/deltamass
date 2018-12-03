package com.dmtavt.deltamass.data;

import com.dmtavt.deltamass.args.DecoyTreatment;
import com.dmtavt.deltamass.predicates.SpmPredicateFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The contents of a proteomics search file.
 */
public class PepSearchFile implements Serializable {

  private static final long serialVersionUID = 8233549653301313773L;
  public final String sourceFile;
  public final List<PepSearchResult> pepSearchResults;

  public PepSearchFile(String sourceFile,
      List<PepSearchResult> pepSearchResults) {
    this.sourceFile = sourceFile;
    this.pepSearchResults = pepSearchResults;
  }

  public static PepSearchFile filter(PepSearchFile psf,
      Double dmLo, Double dmHi, List<Double> excludes, DecoyTreatment decoyTreatment,
      List<SpmPredicateFactory> spfs) {

    ArrayList<PepSearchResult> results = new ArrayList<>(psf.pepSearchResults.size());
    for (PepSearchResult psr : psf.pepSearchResults) {
      PepSearchResult filtered = PepSearchResult
          .filter(psr, dmLo, dmHi, excludes, decoyTreatment, spfs);
      results.add(filtered);
    }

    return new PepSearchFile(psf.sourceFile, results);
  }

}
