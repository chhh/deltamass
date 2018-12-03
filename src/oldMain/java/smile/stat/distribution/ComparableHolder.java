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

package smile.stat.distribution;

/**
 * @author Dmitry Avtonomov
 */
public class ComparableHolder<K extends Comparable<K>, V> implements
    Comparable<ComparableHolder<K, ?>> {

  public final K key;
  public final V val;


  public ComparableHolder(K key, V val) {
    this.key = key;
    this.val = val;
  }

  @Override
  public int compareTo(ComparableHolder<K, ?> o) {
    return key.compareTo(o.key);
  }
}
