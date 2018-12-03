package com.dmtavt.deltamass.utils;

public class GridUtils {
  private GridUtils() {}

  /**
   * Starts at precisely {@code lo} and up to the final segment that includes {@code hi}.
   * @param lo Low point to start from.
   * @param hi High point to go to.
   * @param step Step size.
   * @return A new double[].
   */
  public static double[] grid(double lo, double hi, double step) {
    if (hi < lo)
      throw new IllegalArgumentException("hi < lo");
    if (step <= 0)
      throw new IllegalStateException("step <= 0");
    double span = hi-lo;
    final long n = 1 + (long)Math.ceil(span / step);
    if (n >= Integer.MAX_VALUE)
      throw new IllegalStateException("Resulting number of grid elements over Integer.MAX_VALUE");
    final double[] x = new double[(int)n];
    for (int i = 0; i < n; i++) {
      x[i] = lo + i * step;
    }
    return x;
  }

  /**
   * Create a data grid that has one element matching {@code pivot} exactly. Goes from down from
   * pivot to include {@code lo - margin} and up to include {@code hi + margin}.
   * @param pivot Stationary point that is guaranteed to be in the output grid.
   * @param lo Lowest value to be included.
   * @param hi Highest value to be included.
   * @param step Step size.
   * @param margin Extra margin down from {@code lo} and up from {@code hi}.
   * @return A new double[].
   */
  public static double[] grid(double pivot, double lo, double hi, double step, double margin) {
    if (hi < pivot || pivot < lo)
      throw new IllegalArgumentException("hi < pivot || pivot < lo");
    if (step <= 0)
      throw new IllegalStateException("step <= 0");
    final long nLo = (long)Math.ceil((pivot - lo + margin) / step);
    final long nHi = (long)Math.ceil((hi - pivot + margin) / step);
    final long n = 1 + nLo + nHi;
    if (n >= Integer.MAX_VALUE)
      throw new IllegalStateException("Resulting number of grid elements over Integer.MAX_VALUE");
    final double[] x = new double[(int)n];
    for (int i = 0; i < n; i++) {
      x[i] = pivot + (i - nLo) * step;
    }
    return x;
  }
}
