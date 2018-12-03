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

/**
 * @author Dmitry Avtonomov
 */
public class IndexBracket implements Comparable<IndexBracket> {

  /**
   * Inclusive.
   */
  public final int lo;
  /**
   * Exclusive.
   */
  public final int hi;
  public final double loVal;
  public final double hiVal;
  public final int size;


  public IndexBracket(int lo, int hi, double loVal, double hiVal) {
    this.lo = lo;
    this.hi = hi;
    this.loVal = loVal;
    this.hiVal = hiVal;
    this.size = hi - lo;
  }

  @Override
  public String toString() {
    return "IndexBracket{" +
        "lo=" + lo +
        ", hi=" + hi +
        ", loVal=" + loVal +
        ", hiVal=" + hiVal +
        '}';
  }

  @Override
  public int compareTo(IndexBracket o) {
    return Integer.compare(lo, o.lo);
  }
}
