package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.data.PepSearchResult;
import com.dmtavt.deltamass.data.PepSearchResult.SortOrder;
import com.dmtavt.deltamass.data.Spm;
import com.dmtavt.deltamass.kde.KdeKludge;
import com.github.chhh.utils.search.BinarySearch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import jsat.distributions.empirical.KernelDensityEstimator;
import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.math.Function;
import jsat.math.FunctionVec;
import jsat.math.optimization.BacktrackingArmijoLineSearch;
import jsat.math.optimization.LBFGS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MassCorrectorZeroPeak implements MassCorrector {
  private static final Logger log = LoggerFactory.getLogger(MassCorrectorZeroPeak.class);

  private List<Spm> sublistInMassDiffRange(List<Spm> spms, final double mDiffLo, final double mDiffHi) {
    int loSearch = BinarySearch.search(spms, o -> Double.compare(o.mDiff, mDiffLo));
    int hiSearch = BinarySearch.search(spms, o -> Double.compare(o.mDiff, mDiffHi));
    if (loSearch < 0) loSearch = ~loSearch;
    if (hiSearch < 0) hiSearch = ~hiSearch;
    return spms.subList(loSearch, hiSearch);
  }


  @Override
  public void apply(PepSearchResult pepSearchResult) {
    final double WINDOW = 0.5;
    final double mLo = -1 * WINDOW / 2.0;
    final double mHi = WINDOW / 2.0;
    // the step to find the initial location near which we'll be looking for a solution
    final double stepInitSearch = 0.01;
    // final KernelFunction kernelFunction = UniformKF.getInstance();
    // final KernelFunction kernelFunction = GaussKF.getInstance();
    final KernelFunction kernelFunction = EpanechnikovKF.getInstance();
    final int numBins = (int)Math.ceil((WINDOW * 2) / stepInitSearch);
    if (numBins < 3)
      throw new IllegalStateException("Calculated number of bins for zero-peak mass correction was "
          + "smaller than 3, this should not happen");

    if (pepSearchResult.getSortOrder() != SortOrder.DELTA_MASS_ASCENDING)
      pepSearchResult.sort(SortOrder.DELTA_MASS_ASCENDING);

    final ArrayList<Spm> spms = pepSearchResult.spms;
    List<Spm> selectedSpms = sublistInMassDiffRange(spms, mLo, mHi);
    final int limit = 10;
    if (selectedSpms.size() < limit) {
      log.warn(String.format("Less than %d hits found for zero-peak mass correction in mass range "
          + "[%.2f; %.2f], not performing correction", limit, mLo, mHi));
      pepSearchResult.massCorrection = MassCorrection.ZERO_PEAK;
      return;
    }


    // find the possible location for maximum
    double[] massDiffsArr = new double[selectedSpms.size()];
    for (int i = 0, size = selectedSpms.size(); i < size; i++)
      massDiffsArr[i] = selectedSpms.get(i).mDiff;
    DenseVector massDiffsVec = new DenseVector(massDiffsArr);
    final double bandwidthEstimate = KernelDensityEstimator.BandwithGuassEstimate(massDiffsVec);
    log.debug("Estimated bandwidth for zero-peak correction for '{}', "
        + "obtained a value of {} using gaussian formula", pepSearchResult.rawFileName, bandwidthEstimate);
    final double bandwidthUsed = bandwidthEstimate * 2.0; // relax a little
    final KdeKludge kde = new KdeKludge(massDiffsVec, kernelFunction, bandwidthUsed);


    final double eps = 1e-8d;
    final double maxMassToleranceWanted = 1e-6d;
    final Function func = new Function() {
      @Override public double f(double... x) {
        return -1 * kde.pdf(x[0]);
      }
      @Override public double f(Vec x) {
        return -1 * kde.pdf(x.get(0));
      }
    };
    final FunctionVec funcp = new FunctionVec() {
      @Override public Vec f(double... x) {
        throw new UnsupportedOperationException("Not supported");
      }

      @Override public Vec f(Vec x) {
        DenseVector result = new DenseVector(x.length());
        f(x, result);
        return result;
      }

      @Override public Vec f(Vec x, Vec s) {
        return f(x, s, null);
      }

      @Override public Vec f(Vec x, Vec s, ExecutorService ex) {
        Vec xLo = new DenseVector(x);
        final double delta = maxMassToleranceWanted / 2d;
        xLo = xLo.add(-1d * delta);
        Vec xHi = new DenseVector(x);
        xHi = xHi.add(delta);
        double v = (func.f(xHi) - func.f(xLo)) / (delta * 2d);
        return new DenseVector(Collections.singletonList(v));
      }
    };

    BacktrackingArmijoLineSearch lineSearch = new BacktrackingArmijoLineSearch();
    LBFGS lbfgs = new LBFGS(5, 100, lineSearch);
    Vec lbfgsSolution = new DenseVector(1);
    lbfgs.optimize(1e-12, lbfgsSolution, new DenseVector(Arrays.asList(0.0)), func, funcp, null);
    double mCorrection = lbfgsSolution.get(0);

    log.debug(String.format("ZeroPeak correction: computed for '%s', zero-peak shift of '%.5f' "
        + "using LBFGS", pepSearchResult.rawFileName, mCorrection));
    spms.parallelStream().forEach(spm -> spm.mDiffCorrected = spm.mDiff - mCorrection);

    pepSearchResult.massCorrection = MassCorrection.ZERO_PEAK;
  }
}
