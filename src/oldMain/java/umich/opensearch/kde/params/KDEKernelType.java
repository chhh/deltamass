package umich.opensearch.kde.params;

import jsat.distributions.empirical.kernelfunc.EpanechnikovKF;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.distributions.empirical.kernelfunc.UniformKF;
import umich.opensearch.kde.params.kernels.GaussFasterKF;

/**
 * @author Dmitry Avtonomov
 */
public enum KDEKernelType {
  GAUSS(GaussKF.getInstance()),
  GAUSS_FAST(GaussFasterKF.getInstance()),
  EPANECHNIKOV(EpanechnikovKF.getInstance()),
  UNIFORM(UniformKF.getInstance());

  public final KernelFunction kernel;

  KDEKernelType(KernelFunction kernel) {
    this.kernel = kernel;
  }
}
