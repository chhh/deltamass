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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umich.opensearch.kde.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dmitry Avtonomov
 */
public class ArrayFilter {

  private static final Logger log = LoggerFactory.getLogger(ArrayFilter.class);

  double[] mass;
  double[] ints;

  public ArrayFilter(double[] mass, double[] ints) {
    if (mass.length != ints.length) {
      throw new IllegalArgumentException("Must be equal lengths");
    }
    this.mass = mass;
    this.ints = ints;
  }

  public double[] getMass() {
    return mass;
  }

  public double[] getInts() {
    return ints;
  }

  /**
   * Filter based on zeroes from the second array provided to the constructor.
   *
   * @return New arrays, the original are left intact.
   */
  public ArrayFilter filterZeroesLeaveFlanking() {
    if (ints.length < 5) {
      return this; // don't bother
    }
    double prevInt = ints[0], curInt = ints[1], nextInt;
    int lengthOriginal = ints.length;

    int[] acceptedIndexes = new int[mass.length];
    int acceptedCount = 0;
    acceptedIndexes[0] = 0; // always accept first
    acceptedCount++;

    int i = 1;
    do {
      nextInt = ints[i + 1];
      if ((prevInt == 0 && nextInt == 0) && curInt == 0) {
        // these are being cut out
      } else {
        acceptedIndexes[acceptedCount] = i;
        acceptedCount++;
      }
      prevInt = curInt;
      curInt = nextInt;
      i++;
    } while (i < ints.length - 1);
    acceptedIndexes[acceptedCount] = ints.length - 1; // always accept last
    acceptedCount++;

    double[] massFiltered = new double[acceptedCount];
    double[] intsFiltered = new double[acceptedCount];
    int acceptedIndex;
    for (int j = 0; j < acceptedCount; j++) {
      acceptedIndex = acceptedIndexes[j];
      massFiltered[j] = mass[acceptedIndex];
      intsFiltered[j] = ints[acceptedIndex];
    }
    log.debug("Filtering two double arrays helped remove {} entries. "
            + "Old size {} new size {}.", lengthOriginal - intsFiltered.length, lengthOriginal,
        intsFiltered.length);
    return new ArrayFilter(massFiltered, intsFiltered);
  }

//    public static void main(String[] args) {
//        double[] mass = new double[]{1,2,3,4,5,6};
//        double[] ints = new double[]{1,2,0,0,0,6};
//        ArrayFilter filter = new ArrayFilter(mass, ints);
//        ArrayFilter filtered = filter.filterZeroesLeaveFlanking();
//
//        System.out.printf("Original size: %d\n", filter.getInts().length);
//        System.out.printf("Filtered size: %d\n", filtered.getInts().length);
//    }
}
