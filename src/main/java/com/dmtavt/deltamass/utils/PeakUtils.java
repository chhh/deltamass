package com.dmtavt.deltamass.utils;

import com.dmtavt.deltamass.utils.PeakUtils.Peak.WIDTH;
import com.dmtavt.deltamass.utils.PeakUtils.PeakApprox.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PeakUtils {

  private PeakUtils() {
  }

  /**
   * Finds locations of maxima. Can detect plateaus.
   *
   * @param data Array with data values to detect peaks in.
   * @param from Inclusive.
   * @param to Exclusive.
   * @param filter Filters out peaks before they are added to the resulting list.
   */
  public static List<PeakApprox> peakLocations(double[] data, int from, int to,
      Predicate<PeakApprox> filter) {
    if (from < 0 || from > data.length - 1) {
      throw new IllegalArgumentException("'from' must be within data[] index range");
    }
    if (to < from) {
      throw new IllegalArgumentException("'to' must be >= 'from'");
    }
    if (data.length < 3) {
      return Collections.emptyList();
    }

    List<PeakApprox> peaks = new ArrayList<>(Math.min(data.length / 10, 64));
    double diff;
    PeakApprox p = null;
    State s0 = PeakApprox.State.FLAT, s1;

    int lastFlatStart = -1;
    for (int i = from; i < to - 1;
        i++) // -1 because to is exclusive and we're comparing to i+1 data point at each step
    {
      int ip1 = i + 1;
      diff = data[ip1] - data[i];
      if (diff > 0) {
        s1 = PeakApprox.State.UP;
      } else if (diff < 0) {
        s1 = PeakApprox.State.DOWN;
      } else {
        s1 = PeakApprox.State.FLAT;
      }
      if (s1 == PeakApprox.State.FLAT && s0 != PeakApprox.State.FLAT) {
        lastFlatStart = i;
      }

      switch (s1) {
        case UP:
          if (p == null) {
            p = peakStart(i);
          } else if (p.idxHi != p.idxTopHi) {
            peakFinish(data, filter, peaks, p, lastFlatStart);
            p = peakStart(i);
          }
          p.idxTopLo = ip1;
          p.idxTopHi = ip1;
          p.idxHi = ip1;
          break;

        case FLAT:
          if (p == null) {
            break;
          }
          if (p.idxTopHi == p.idxHi) {
            p.idxTopHi = ip1;
          }
          p.idxHi = ip1;
          break;

        case DOWN:
          if (p == null) {
            break;
          }
          p.idxHi = ip1;
          break;
      }

      s0 = s1;
    }
    if (p != null) {
      peakFinish(data, filter, peaks, p, lastFlatStart);
    }

    return peaks;
  }

  /**
   * Starts a new peak and initializes its {@code idxLo} and {@code valLo} fields.
   *
   * @param i Current pointer in the data array.
   * @return A new peak object.
   */
  private static PeakApprox peakStart(int i) {
    PeakApprox p = new PeakApprox();
    p.idxLo = i;

    return p;
  }

  /**
   * Add the peak to a list if it passes certain criteria.
   *
   * @param data The data array.
   * @param filter Options for peak detection.
   * @param toAddTo The list to add the peak to if it passes quality checks.
   * @param p The peak to be finalized.
   * @param lastFlatStart The index where the last flat area started. If it's between {@code
   * idxTopHi} and {@code idxHi}, it will be used instead of {@code idxHi}
   * @return True, if the peak passed criteria in {@code opts} and was added to {@code toAddTo}.
   */
  private static boolean peakFinish(double[] data, Predicate<PeakApprox> filter,
      List<PeakApprox> toAddTo, PeakApprox p, int lastFlatStart) {
    if (lastFlatStart > p.idxTopHi && lastFlatStart < p.idxHi) {
      p.idxHi = lastFlatStart;
    }
    p.valLo = data[p.idxLo];

    p.valTop = data[p.idxTopLo];
    p.valHi = data[p.idxHi];
    p.numNonZeroPts = p.idxHi - p.idxLo + 1;
    if (data[p.idxLo] == 0) {
      p.numNonZeroPts--;
    }
    if (data[p.idxHi] == 0) {
      p.numNonZeroPts--;
    }
    if (!filter.test(p)) {
      return false;
    }
    toAddTo.add(p);
    return true;
  }

  public static Peak fitPeakByParabola(PeakApprox peakApprox, double[] xVals, double[] yVals,
      boolean useYRangeAsHeight) {

    double vertexX;
    if (peakApprox.idxTopHi == peakApprox.idxTopLo) {
      int idx = peakApprox.idxTopLo;
      if (idx > 0 && idx < xVals.length - 1) {
        double[] x = new double[3];
        double[] y = new double[3];
        for (int i = -1; i <= 1; i++) {
          x[i + 1] = xVals[idx + i];
          y[i + 1] = yVals[idx + i];
        }
        double[] parabola = fitParabola(x, y);
        vertexX = parabolaVertexX(parabola[2], parabola[1]);
      } else {
        vertexX = xVals[peakApprox.idxTopLo];
      }

    } else {
      int idx = peakApprox.idxTopLo;
      if (idx > 0) {
        double[] x = new double[3];
        double[] y = new double[3];
        x[0] = xVals[idx - 1];
        y[0] = yVals[idx - 1];
        x[1] = xVals[idx];
        y[1] = yVals[idx];
        x[2] = xVals[peakApprox.idxTopHi];
        y[2] = yVals[peakApprox.idxTopHi];

        double[] parabola = fitParabola(x, y);
        vertexX = parabolaVertexX(parabola[2], parabola[1]);
      } else {
        vertexX = xVals[peakApprox.idxTopLo];
      }

    }

    double height;
    if (useYRangeAsHeight) {
      height = peakApprox.valTop - Math.max(peakApprox.valLo, peakApprox.valHi);
    } else {
      height = peakApprox.valTop;
    }

    double width = xVals[peakApprox.idxHi] - xVals[peakApprox.idxLo];

    Peak p = new Peak(vertexX, height, width, WIDTH.AT_BASE);
    return p;
  }

  /**
   * 3-point fit of a parabola.
   *
   * @param x the x coordinates.
   * @param y the y coordinates.
   * @return double[]: [0] - c, [1] - b, [2] - a
   */
  public static double[] fitParabola(double[] x, double[] y) {
    if (x.length != 3) {
      throw new IllegalArgumentException("Length of input data arrays must be 3.");
    }
    if (x.length != y.length) {
      throw new IllegalArgumentException("Arrays x and y must be of the same length: 3.");
    }
    double a = (y[1] * (x[2] - x[0]) - y[0] * (x[2] - x[1]) - y[2] * (x[1] - x[0])) / (
        Math.pow(x[0], 2) * (x[1] - x[2]) - Math.pow(x[2], 2) * (x[1] - x[0])
            - Math.pow(x[1], 2) * (x[0] - x[2]));
    double b = (y[1] - y[0] + a * (Math.pow(x[0], 2) - Math.pow(x[1], 2))) / (x[1] - x[0]);
    double c = -1 * a * Math.pow(x[0], 2) - b * x[0] + y[0];
    double[] parabola = new double[3];
    parabola[0] = c;
    parabola[1] = b;
    parabola[2] = a;
    return parabola;
  }

  public static double parabolaVertexX(double a, double b) {
    return -1 * b / (2.0 * a);
  }

  /**
   * Approximate peak location in a 1D array of values. Stores the start and finish locations in the
   * array as well as the corresponding values at the start, finish and at the top.
   *
   * @author Dmitry Avtonomov
   */
  public static class PeakApprox {

    public int idxLo = -1;
    public int idxHi = -1;
    public int idxTopLo = -1;
    public int idxTopHi = -1;
    public double valLo = Double.NEGATIVE_INFINITY;
    public double valTop = Double.NEGATIVE_INFINITY;
    public double valHi = Double.NEGATIVE_INFINITY;

    public int numNonZeroPts = 0;
    public int idxLoNonZero = -1;
    public int idxHiNonZero = -1;

    public double mzInterpolated;

    public void flipValSign() {
      valLo = -1 * valLo;
      valTop = -1 * valTop;
      valHi = -1 * valHi;
    }

    public double amplitudeLo() {
      return Math.min(Math.abs(valTop - valLo), Math.abs(valTop - valHi));
    }

    public double amplitudeHi() {
      return Math.max(Math.abs(valTop - valLo), Math.abs(valTop - valHi));
    }

    public double amplitudeMax() {
      return Math.max(amplitudeLo(), amplitudeHi());
    }

    enum State {UP, DOWN, FLAT}
  }

  public static class PeakDetectionConfig {

    public final double minPeakPct;
    public final double minPsmsPerGmm;

    public PeakDetectionConfig(double minPeakPct, double minPsmsPerGmm) {
      this.minPeakPct = minPeakPct;
      this.minPsmsPerGmm = minPsmsPerGmm;
    }
  }

  public static class Peak {

    public final double location;
    public final double intensity;
    public final double width;
    public final WIDTH widthType;

    public Peak(double location, double intensity, double width,
        WIDTH widthType) {
      this.location = location;
      this.intensity = intensity;
      this.width = width;
      this.widthType = widthType;
    }

    public enum WIDTH {AT_BASE, FWHM}
  }
}
