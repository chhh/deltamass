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

package umich.opensearch.kde.api;

import java.io.Serializable;

/**
 * Pairs can be compared based on their first value.
 */
public class Pair<T extends Comparable<T>> implements Comparable<Pair<T>>, Serializable {

  private static final long serialVersionUID = 200541829177602541L;
  public final T v1;
  public final T v2;

  public Pair(T v1, T v2) {
    this.v1 = v1;
    this.v2 = v2;
  }

  @Override
  public int compareTo(Pair<T> o) {
    int result = v1.compareTo(o.v1);
    if (result == 0) {
      result = v2.compareTo(o.v2);
    }
    return result;
  }
}
