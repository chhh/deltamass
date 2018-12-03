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

package umich.opensearch.kde.impl;

import java.util.ArrayList;
import umich.opensearch.kde.api.IWeightedData;

/**
 * @author Dmitry Avtonomov
 */
public class WeightedDataDelimitedFile implements IWeightedData {

  private double[] diffs;
  private double[] weights = null;
  private WeightedDataDelimitedFile() {
  }

  public static WeightedDataDelimitedFile create(ArrayList<double[][]> inputData) {
    if (inputData.isEmpty()) {
      throw new IllegalArgumentException("input data can't be empty");
    }
    boolean hasWeights = inputData.get(0).length > 1;
    int size = 0;
    for (double[][] doubles : inputData) {
      size += doubles[0].length;
    }
    WeightedDataDelimitedFile result = new WeightedDataDelimitedFile();
    result.diffs = new double[size];
    if (hasWeights) {
      result.weights = new double[size];
    }
    int idx = 0;
    for (double[][] doubles : inputData) {
      double[] diffs = doubles[0];
      double[] weights = hasWeights ? doubles[1] : null;
      for (int i = 0; i < diffs.length; i++) {
        result.diffs[idx] = diffs[i];
        if (result.weights != null) {
          result.weights[idx] = weights[i];
        }
        idx++;
      }
    }
    return result;
  }

  @Override
  public double[] getData() {
    return new double[0];
  }

  @Override
  public double[] getWeights() {
    return new double[0];
  }
}
