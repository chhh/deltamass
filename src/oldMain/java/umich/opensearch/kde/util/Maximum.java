package umich.opensearch.kde.util;

/**
 * @author Dmitry Avtonomov
 */
public class Maximum {

  public int idxLo;
  public int idxHi;
  public double xLo;
  public double xHi;
  public double val;

  public Maximum(int idxdxLo, int idxHi, double xLo, double xHi, double val) {
    this.idxLo = idxdxLo;
    this.idxHi = idxHi;
    this.xLo = xLo;
    this.xHi = xHi;
    this.val = val;
  }
}
