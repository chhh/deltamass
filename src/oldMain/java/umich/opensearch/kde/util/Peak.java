package umich.opensearch.kde.util;

/**
 * A peak at location x of height y. Optionally with a width and the parent {@link PeakApprox}.
 *
 * @author Dmitry Avtonomov
 */
public class Peak implements Comparable<Peak> {

  public final double x;
  public final double y;
  public final double width;
  public final WIDTH widthType;
  public final PeakApprox peak;


  public Peak(double x, double y) {
    this.x = x;
    this.y = y;
    this.width = Double.NaN;
    this.widthType = WIDTH.NONE;
    this.peak = null;
  }

  public Peak(PeakApprox peak, double x, double y, double width, WIDTH type) {
    this.peak = peak;
    this.x = x;
    this.y = y;
    this.width = width;
    this.widthType = type;
  }

  @Override
  public int compareTo(Peak o) {
    return Double.compare(x, o.x);
  }

  public enum WIDTH {FULL, FWHM, SD, NONE}
}
