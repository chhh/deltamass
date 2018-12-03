package com.dmtavt.deltamass.kde;

import com.github.chhh.utils.MathUtils;
import java.util.ArrayList;
import jsat.distributions.empirical.KernelDensityEstimator;
import jsat.linear.DenseVector;

/**
 * @author Dmitry Avtonomov
 */
public class KdeUtils {
  private KdeUtils() {}


  /**
   * Estimate bandwidth for KDE using Gaussian kernel method.
   * @param massDiffs an array of all the mass diffs that were there
   * @param nearestPeak near which mass will we be searching for a single good peak to estimate bandwidth
   * @param maxDistance the max distance from the {@code nearestPeak} to accept a point for the estimate
   *                 (this will be used as +/- distance)
   * @return estimated bandwidth
   */
  public static double estimateBandwidth(DenseVector massDiffs, double nearestPeak, double maxDistance) {
    if (massDiffs.length() < 3) {
      return 0.5; // slightly better than returning 1 by default
    }
    ArrayList<Double> accepted = new ArrayList<>(massDiffs.length() / 2);
    for (int i = 0; i < massDiffs.length(); i++) {
      double diff = massDiffs.get(i);
      if (MathUtils.isWithinAbs(diff, nearestPeak, maxDistance)) {
        accepted.add(diff);
      }
    }
    DenseVector acceptedVec = new DenseVector(accepted);
    return KernelDensityEstimator.BandwithGuassEstimate(acceptedVec);
  }
}
