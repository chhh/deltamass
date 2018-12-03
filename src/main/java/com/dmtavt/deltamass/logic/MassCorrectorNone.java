package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.data.PepSearchResult;

/**
 * Resets corrected mass diff to its original raw value.
 */
public class MassCorrectorNone implements MassCorrector {

  @Override
  public void apply(PepSearchResult pepSearchResult) {
    pepSearchResult.spms.parallelStream().forEach(spm -> spm.mDiffCorrected = spm.mDiff);
    pepSearchResult.massCorrection = MassCorrection.NONE;
  }
}
