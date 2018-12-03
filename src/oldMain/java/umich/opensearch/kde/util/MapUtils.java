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

import java.util.Map;
import java.util.NavigableMap;

/**
 * @author Dmitry Avtonomov
 */
public class MapUtils {

  private MapUtils() {
  }

  /**
   * Find the closest entry in a map.
   *
   * @param map The map to search in.
   * @param target The target value for lookup.
   * @param <T> Type of mapping in the map.
   * @return Null if nothing found or the closest value. If two map entries are at the same
   * distance, the lower one is returned.
   */
  public static <T> T findClosest(NavigableMap<Double, T> map, double target) {
    if (map.isEmpty()) {
      return null;
    }
    final Map.Entry<Double, T> ceil = map.ceilingEntry(target);
    final Map.Entry<Double, T> floor = map.floorEntry(target);
    if (ceil != null) {
      if (floor == null) {
        return ceil.getValue();
      }
      // both non null, find closest
      if (Math.abs(floor.getKey() - target) <= Math.abs(ceil.getKey() - target)) {
        return floor.getValue();
      }
      return ceil.getValue();

    } else { // ceil == null
      if (floor != null) {
        return floor.getValue();
      }
      throw new IllegalStateException("ceil == null && floor == null, should not happen");
    }
  }
}
