package umich.opensearch.kde.params.kernels;

import jsat.distributions.empirical.kernelfunc.KernelFunction;

/**
 * @author Dmitry Avtonomov
 */
public interface KernelFuncPrime2 extends KernelFunction {

  /**
   * Second derivative at point U.
   *
   * @param u point for derivative calcualtion
   */
  double kPrime2(double u);

  /**
   * True if the value for second derivative is calculated analytically.
   */
  boolean isKPrime2Exact();
}
