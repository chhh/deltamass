package com.dmtavt.deltamass.data;

import java.io.Serializable;

/**
 * Spectrum-to-peptide match. Comparisons are based on mass diff only!
 */
public class Spm implements Serializable, Comparable<Spm> {

  private static final long serialVersionUID = 2974262482081903368L;

  public String spectrumId = "";
  public String seq;
  public String seqModStateId;
  public String mods;
  public int protId;
  public int[] protIdAlt;
  public int charge;
  public double mObsNeutral;
  public double mzObs;
  public double mCalcNeutral;
  public double mDiff;
  public double mDiffCorrected;
  public double rtSec = Double.NaN;
  public double[] scores;
  public boolean isDecoy;

  public Spm(int numScores) {
    scores = new double[numScores];
  }

  @Override
  public String toString() {
    return "{" +
        "Mcalc=" + String.format("%.5f", mCalcNeutral) +
        ", Mobs=" + String.format("%.5f", mObsNeutral) +
        ", dM=" + String.format("%.5f", mDiff) +
        ", Z=" + charge +
        ", Seq='" + seq + '\'' +
        '}';

  }

  @Override
  public int compareTo(Spm o) {
    return Double.compare(mDiff, o.mDiff);
  }

}
