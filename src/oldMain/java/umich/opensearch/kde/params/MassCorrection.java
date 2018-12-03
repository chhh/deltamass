package umich.opensearch.kde.params;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
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
import umich.opensearch.kde.api.SearchHitDiff;
import umich.opensearch.kde.api.SearchHitDiffs;
import umich.opensearch.kde.jsat.KDEKludge;
import umich.opensearch.kde.jsat.KDEUtils;

/**
 * @author Dmitry Avtonomov
 */
public enum MassCorrection {
  NONE,
  ZERO_PEAK,
  CAL_FILES;

  private static final Logger log = LoggerFactory.getLogger(MassCorrection.class);


  /**
   * Some correction converters might incur calling sorting by mass diff.
   *
   * @param shds The diffs to be corrected (in place). The mass correction type will be set as
   * well.
   * @param type Mass correction type.
   * @param massCal Can be null. Needed only for {@link MassCorrection#CAL_FILES}.
   */
  public static void correctMasses(SearchHitDiffs shds, MassCorrection type,
      HashMap<String, Double> massCal) {
    log.info("Running mass correction for '{}' using correction type '{}'", shds.getName(),
        type.toString());
    switch (type) {
      case ZERO_PEAK:
        final double ZERO_PEAK_WINDOW = 0.5;
        final double mLo = -1 * ZERO_PEAK_WINDOW;
        final double mHi = ZERO_PEAK_WINDOW;
        final double stepInitSearch = 0.01; // we'll use this step to find the initial location near which we'll be looking for a solution
//                final KernelFunction kernelFunction = UniformKF.getInstance();
        final KernelFunction kernelFunction = EpanechnikovKF.getInstance();
//                final KernelFunction kernelFunction = GaussKF.getInstance();
        final int numBins = (int) Math.ceil((ZERO_PEAK_WINDOW * 2) / stepInitSearch);
        if (numBins < 3) {
          String msg = "Calculated number of bins for zero-peak mass correction was smaller than 3, this should not happen";
          log.error(msg);
          throw new IllegalStateException(msg);
        }
        if (!shds.isSorted()) {
          shds.sortByDiffMass();
        }
        List<SearchHitDiff> diffsByMassDiffRange = shds.findDiffsByMassDiffRange(mLo, mHi);
        if (diffsByMassDiffRange.size() < 10) {
          log.warn(
              "Less than 10 hits found for zero-peak mass correction in mass range [-0.5; 0.5], not doing any correction");
          return;
        }

        // find the possible location for maximum
        double[] massDiffsArray = SearchHitDiffs.getMassDiffsArray(diffsByMassDiffRange);
        DenseVector massDiffsVec = new DenseVector(massDiffsArray);
        double bandwidth = KDEUtils.estimateBandwidth(massDiffsVec, 0.0, ZERO_PEAK_WINDOW);
        log.debug(
            "Estimated bandwidth for zero-peak correction for '{}', obtained a value of {} using gaussian formula",
            shds.getName(), bandwidth);
        bandwidth = bandwidth * 2; // relax a little
        final KDEKludge kde = new KDEKludge(massDiffsVec, kernelFunction, bandwidth);

        final double eps = 1e-8d;
        final double maxMassToleranceWanted = 1e-6d;
        final Function func = new Function() {
          @Override
          public double f(double... x) {
            return -1 * kde.pdf(x[0]);
          }

          @Override
          public double f(Vec x) {
            return -1 * kde.pdf(x.get(0));
          }
        };
        final FunctionVec funcp = new FunctionVec() {
          @Override
          public Vec f(double... x) {
            throw new UnsupportedOperationException("Not supported");
          }

          @Override
          public Vec f(Vec x) {
            DenseVector result = new DenseVector(x.length());
            f(x, result);
            return result;
          }

          @Override
          public Vec f(Vec x, Vec s) {
            return f(x, s, null);
          }

          @Override
          public Vec f(Vec x, Vec s, ExecutorService ex) {
            Vec xLo = new DenseVector(x);
            final double delta = maxMassToleranceWanted / 2d;
            xLo = xLo.add(-1d * delta);
            Vec xHi = new DenseVector(x);
            xHi = xHi.add(delta);
            double v = (func.f(xHi) - func.f(xLo)) / (delta * 2d);
            return new DenseVector(Arrays.asList(v));
          }
        };

        BacktrackingArmijoLineSearch lineSearch = new BacktrackingArmijoLineSearch();
        LBFGS lbfgs = new LBFGS(5, 100, lineSearch);
        Vec lbfgsSolution = new DenseVector(1);
        lbfgs
            .optimize(1e-12, lbfgsSolution, new DenseVector(Arrays.asList(0.0)), func, funcp, null);
        double mCorrection = lbfgsSolution.get(0);

        log.debug(String
            .format("ZeroPeak correction: computed for '%s', zero-peak shift of '%.5f' using LBFGS",
                shds.getName(), mCorrection));

        List<SearchHitDiff> hits = shds.getHits();
        double newDiffVal;
        for (SearchHitDiff hit : hits) {
          newDiffVal = hit.getMassDiff() - mCorrection;
          hit.setMassDiffCal(newDiffVal);
        }
        shds.setMassCorrection(MassCorrection.ZERO_PEAK);

        break;

      case CAL_FILES:
        if (massCal == null) {
          throw new IllegalArgumentException(
              "When calling mass correction for CAL_FILES, the mass correction mapping must be provided. "
                  +
                  "See PepXmlContent HashMap<String, Double> parseMassCorrectionFile()");
        }
        for (SearchHitDiff shd : shds.getHits()) {
          String id = shd.getSpectrumId();
          Double calibrated = massCal.get(id);
          if (calibrated == null) {
            shd.setMassDiffCal(shd.getMassDiff());
            log.warn("Could not find calibrated mass for spectrum query (" + id
                + "), setting diff to uncalibrated mass.");
          } else {
            shd.setMassDiffCal(calibrated);
          }
        }
        shds.setMassCorrection(MassCorrection.CAL_FILES);

        break;

      case NONE:
        break;

      default:
        throw new IllegalArgumentException(
            "MassCorrection " + type.toString() + " is not supported in this method.");
    }
  }

}
