/*
 * Copyright (c) 2016 Dmitry Avtonomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package umich.opensearch.kde.util;

public class PeakProminence {

  private PeakProminence() {
  }

  public static double pp(double[] a) {
    int[] mp = maxp(a, 1);
    return pp(a, mp[0]);
  }

  public static double pp(double[] a, int mpos) {
    double[] x = new double[a.length];
    for (int i = 0; i < a.length; ++i) {
      x[i] = (double) i;
    }

    double[] lp = fit(x, a, mpos);

    // compute the prominence, i.e., the distance between the regression
    // line and the actual value
    return Math.abs(a[mpos] - lp[0] + mpos * lp[1]);
  }

  /**
   * Estimate the parameters of a straight line, ignoring the indicated value
   *
   * @return offset, slope
   */
  public static double[] fit(double[] x, double[] y, int skip) {
    double s = 0.0, sx = 0.0, sy = 0.0, sxx = 0.0, sxy = 0.0, del;

    s = x.length;
    for (int i = 0; i < x.length; i++) {
      if (i == skip) {
        continue;
      }
      sx += x[i];
      sy += y[i];
      sxx += x[i] * x[i];
      sxy += x[i] * y[i];
    }

    del = s * sxx - sx * sx;

    double[] pout = new double[2];
    pout[0] = (sxx * sy - sx * sxy) / del;
    pout[1] = (s * sxy - sx * sy) / del;

    return pout;
  }

  /**
   * Find and return the indices of the maximum values in a descending order regarding the max
   * values
   */
  public static int[] maxp(double[] values, int n) {
    int[] p = new int[n];
    double[] v = new double[n];
    max(values, p, v);
    return p;
  }

  /**
   * Find and locate the maxima and write them to the pos and val array. The output is sorted
   * (descending).
   */
  public static void max(double[] in, int[] pos, double[] val) {
    int u = 0;
    int n = pos.length;

    for (int i = 0; i < in.length; ++i) {
      double v = in[i];

      // locate the insert position
      int p = 0;
      while (p < u && v < val[p]) {
        p++;
      }

      // v is smaller than all other values
      if (p == n) {
        continue;
      }

      // shift the old values and indices
      for (int j = u - 2; j >= p; --j) {
        pos[j + 1] = pos[j];
        val[j + 1] = val[j];
      }

      // insert
      pos[p] = i;
      val[p] = v;

      // keep track how many slots are used already
      if (u < n) {
        u++;
      }
    }
  }
}
