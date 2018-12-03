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
import umich.opensearch.kde.api.Pair;

/**
 * @author Dmitry Avtonomov
 */
public final class DistributionResult {

  int numWorkers;
  double valLo;
  double valHi;
  double stepSize;
  int maxStepNum;
  /**
   * This is the value, that is calculated based on the stepSize and valLo and valHi. valLo and
   * valHi must be covered by interval [startVal; endVal]
   */
  double startVal;
  /**
   * This is the value, that is calculated based on the stepSize and valLo and valHi. valLo and
   * valHi must be covered by interval [startVal; endVal]
   */
  double endVal;
  ArrayList<Pair<Double>> workerIntervals;

  public int getMaxStepNum() {
    return maxStepNum;
  }

  public void setMaxStepNum(int maxStepNum) {
    this.maxStepNum = maxStepNum;
  }

  public int getNumWorkers() {
    return numWorkers;
  }

  public void setNumWorkers(int numWorkers) {
    this.numWorkers = numWorkers;
  }

  public double getValLo() {
    return valLo;
  }

  public void setValLo(double valLo) {
    this.valLo = valLo;
  }

  public double getValHi() {
    return valHi;
  }

  public void setValHi(double valHi) {
    this.valHi = valHi;
  }

  public double getStepSize() {
    return stepSize;
  }

  public void setStepSize(double stepSize) {
    this.stepSize = stepSize;
  }

  public double getStartVal() {
    return startVal;
  }

  public void setStartVal(double startVal) {
    this.startVal = startVal;
  }

  public double getEndVal() {
    return endVal;
  }

  public void setEndVal(double endVal) {
    this.endVal = endVal;
  }

  public ArrayList<Pair<Double>> getWorkerIntervals() {
    return workerIntervals;
  }

  public void setWorkerIntervals(ArrayList<Pair<Double>> workerIntervals) {
    this.workerIntervals = workerIntervals;
  }
}
