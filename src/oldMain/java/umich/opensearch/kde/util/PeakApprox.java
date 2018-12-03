/*
 * Copyright (c) 2017 Dmitry Avtonomov
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

/**
 * Approximate peak location in a 1D array of values. Stores the start and finish locations in the
 * array as well as the corresponding values at the start, finish and at the top.
 *
 * @author Dmitry Avtonomov
 */
public class PeakApprox {

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
}
