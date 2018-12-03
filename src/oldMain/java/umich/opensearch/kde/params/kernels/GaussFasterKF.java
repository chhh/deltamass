package umich.opensearch.kde.params.kernels;

import jsat.distributions.Normal;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;

/**
 * @author Dmitry Avtonomov
 */
public class GaussFasterKF implements KernelFuncPrime2 {

  private GaussFasterKF() {
  }

  /**
   * Returns the singleton instance of this class
   *
   * @return the instance of this class
   */
  public static GaussFasterKF getInstance() {
    return SingletonHolder.INSTANCE;
  }

  @Override
  public double k(double u) {
    return 1 / FastMath.sqrt(MathUtils.TWO_PI) * FastMath.exp(-FastMath.pow(u, 2) / 2);
  }

  @Override
  public double intK(double u) {
    return Normal.cdf(u, 0, 1);
  }

  @Override
  public double k2() {
    return 1;
  }

  @Override
  public double cutOff() {
    /*
     * This is not techincaly correct, as this value of k(u) is still 7.998827757006813E-38
     * However, this is very close to zero, and is so small that  k(u)+x = x, for most values of x.
     * Unless this probability si going to be near zero, values past this point will have
     * no effect on the result
     */
    return 13;
  }

  @Override
  public double kPrime(double u) {
    return -u * k(u);
  }

  @Override
  public double kPrime2(double u) {
    return (FastMath.pow(u, 2) - 1) * k(u);
  }

  @Override
  public boolean isKPrime2Exact() {
    return true;
  }

  @Override
  public String toString() {
    return "Gaussian Kernel using Apache Commons Math3 library";
  }

  private static class SingletonHolder {

    public static final GaussFasterKF INSTANCE = new GaussFasterKF();
  }

}

