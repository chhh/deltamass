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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jsat.linear.DenseVector;

/**
 * @author Dmitry Avtonomov
 */
public class BracketUtils {

  /**
   * Finds index brackets for values which are within maxDist of integer values.
   *
   * @param maxDist max distance from the integer value
   * @param data array to search in
   * @return lo index is inclusive, hi index is exclusive.
   */
  public static List<IndexBracket> findIntegerBrackets(double maxDist, double[] data) {
    List<IndexBracket> brackets = new ArrayList<>();
    if (data.length == 0) {
      return brackets;
    }
    int loInt = (int) (data[0] + 0.5);
    int hiInt = (int) (data[data.length - 1] + 0.5);
    for (int i = loInt; i <= hiInt; i++) {
      double loTarget = i - maxDist;
      double hiTarget = i + maxDist;
      IndexBracket bracket = findBracket(loTarget, hiTarget, data);
      if (bracket.size != 0) {
        brackets.add(bracket);
      }
    }

    return brackets;
  }

  public static IndexBracket findBracket(double loTarget, double hiTarget, double[] data) {
    if (hiTarget < loTarget) {
      throw new IllegalArgumentException("HI target must be greater or equal to LO target.");
    }
    int loVecIdx = Arrays.binarySearch(data, loTarget);
    if (loVecIdx < 0) {
      loVecIdx = ~loVecIdx;
    } else {
      // if we found the exact value, we still need to check to the left, there might be repeating values
      double origValue = data[loVecIdx];
      for (int i = loVecIdx - 1; i >= 0; i--) {
        if (data[i] == origValue) {
          loVecIdx = i;
        } else {
          break;
        }
      }
    }
    int hiVecIdx = Arrays.binarySearch(data, hiTarget);
    if (hiVecIdx < 0) {
      hiVecIdx = ~hiVecIdx;
    } else {
      double origValue = data[hiVecIdx];
      for (int i = hiVecIdx + 1; i < data.length; i++) {
        if (data[i] == origValue) {
          hiVecIdx = i;
        } else {
          break;
        }
      }
    }
    if (hiVecIdx < loVecIdx) {
      throw new IllegalStateException(
          "Input data array was likely not sorted, which resulted in incorrect binary search.");
    }
    if (hiVecIdx == loVecIdx) {
      return new IndexBracket(loVecIdx, hiVecIdx, Double.NaN, Double.NaN);
    }
    return new IndexBracket(loVecIdx, hiVecIdx, data[loVecIdx], data[hiVecIdx - 1]);
  }

  public static IndexBracket findBracket(double loTarget, double hiTarget, DenseVector data) {
    if (hiTarget < loTarget) {
      throw new IllegalArgumentException("HI target must be greater or equal to LO target.");
    }
    int loVecIdx = binarySearch(data, 0, data.length(), loTarget);
    if (loVecIdx < 0) {
      loVecIdx = ~loVecIdx;
    } else {
      // if we found the exact value, we still need to check to the left, there might be repeating values
      double origValue = data.get(loVecIdx);
      for (int i = loVecIdx - 1; i >= 0; i--) {
        if (data.get(i) == origValue) {
          loVecIdx = i;
        } else {
          break;
        }
      }
    }
    int hiVecIdx = binarySearch(data, 0, data.length(), hiTarget);
    if (hiVecIdx < 0) {
      hiVecIdx = ~hiVecIdx;
    } else {
      double origValue = data.get(hiVecIdx);
      for (int i = hiVecIdx + 1; i < data.length(); i++) {
        if (data.get(i) == origValue) {
          hiVecIdx = i;
        } else {
          break;
        }
      }
    }
    if (hiVecIdx < loVecIdx) {
      throw new IllegalStateException(
          "Input data array was likely not sorted, which resulted in incorrect binary search.");
    }
    if (hiVecIdx == loVecIdx) {
      return new IndexBracket(loVecIdx, hiVecIdx, Double.NaN, Double.NaN);
    }
    return new IndexBracket(loVecIdx, hiVecIdx, data.get(loVecIdx), data.get(hiVecIdx - 1));
  }

  public static double[] createArraySliceCopy(IndexBracket b, double[] data) {
    return Arrays.copyOfRange(data, b.lo, b.hi);
  }

  /**
   * Linear interpolation based on a precomputed array of data.
   *
   * @param xAxis The data X array.
   * @param yAxis The data Y array.
   * @param x The point to calculate interpolation at. If outside of data range
   * IllegalArgumentException is thrown.
   * @return Linearly interpolated value.
   */
  public static double interpolateLinear(final double[] xAxis, final double[] yAxis,
      final double x) {
    if (x < xAxis[0] || x > xAxis[xAxis.length - 1]) {
      throw new IllegalArgumentException("x must be within xAxis range.");
    }
    int pos = Arrays.binarySearch(xAxis, x);
    if (pos >= 0) {
      return yAxis[pos];
    }

    pos = ~pos;
    if (pos == 0) {
      return yAxis[0];
    }
    if (pos == xAxis.length - 1) {
      return yAxis[xAxis.length - 1];
    }

    // value at pos is higher than the one searched for
    double span = xAxis[pos] - xAxis[pos - 1];
    double w0 = (xAxis[pos] - x) / span;
    double w1 = (x - xAxis[pos - 1]) / span;
    double interp = w0 * yAxis[pos - 1] + w1 * yAxis[pos];
    return interp;
  }

  public static int binarySearch(DenseVector vec, int from, int to, double target) {
    int low = from;
    int high = to - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      double midVal = vec.get(mid);

      if (midVal < target) {
        low = mid + 1;
      } else if (midVal > target) {
        high = mid - 1;
      } else {
        long midBits = Double.doubleToLongBits(midVal);
        long keyBits = Double.doubleToLongBits(target);
        if (midBits == keyBits) {
          return mid;
        } else if (midBits < keyBits) {
          low = mid + 1;
        } else {
          high = mid - 1;
        }
      }
    }
    return -(low + 1);
  }

  /**
   * Y axis is searched for gaps of zeros and corresponding X axis ranges are reported (for non-zero
   * regions).
   *
   * @param from Inclusive.
   * @param to Exclusive.
   */
  public static List<IndexBracket> findNonZeroBrackets(double[] xAxis, double[] yAxis, int from,
      int to) {
    ArrayList<IndexBracket> res = new ArrayList<>();
    int lo = -1;
    for (int i = 0; i < xAxis.length; i++) {
      double y = yAxis[i];
      if (lo < 0) {
        if (y > 0) {
          lo = i;
        }
      } else if (y == 0) {
        IndexBracket b = new IndexBracket(lo, i, xAxis[lo], xAxis[i - 1]);
        res.add(b);
        lo = -1;
      }
    }
    if (lo >= 0) {
      int hi = xAxis.length;
      IndexBracket b = new IndexBracket(lo, hi, xAxis[lo], xAxis[hi - 1]);
      res.add(b);
    }
    return res;
  }
}
