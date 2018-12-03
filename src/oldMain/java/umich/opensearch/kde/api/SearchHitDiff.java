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
 * @author Dmitry Avtonomov
 */
public class SearchHitDiff implements Serializable, Comparable<SearchHitDiff> {

  private static final long serialVersionUID = -7250766622941392404L;

  String spectrumId = "";
  double massDiff;
  double massDiffCal = Double.NaN;
  String seq;
  int charge;
  double observedNeutralMass;
  double calcedNeutralMass;
  double rtSec = Double.NaN;
  int scanNum = Integer.MIN_VALUE;
  double[] scores;
  boolean isDecoy;

  public SearchHitDiff(int numScores) {
    scores = new double[numScores];
  }

  public String getSpectrumId() {
    return spectrumId;
  }

  public void setSpectrumId(String spectrumId) {
    this.spectrumId = spectrumId;
  }

  public double getMassDiffCal() {
    return massDiffCal;
  }

  public void setMassDiffCal(double massDiffCal) {
    this.massDiffCal = massDiffCal;
  }

  public double getRtSec() {
    return rtSec;
  }

  public void setRtSec(double rtSec) {
    this.rtSec = rtSec;
  }

  public int getScanNum() {
    return scanNum;
  }

  public void setScanNum(int scanNum) {
    this.scanNum = scanNum;
  }

  public double[] getScores() {
    return scores;
  }

  public boolean isDecoy() {
    return isDecoy;
  }

  public void setDecoy(boolean isDecoy) {
    this.isDecoy = isDecoy;
  }

  public double getMassDiff() {
    return massDiff;
  }

  public void setMassDiff(double massDiff) {
    this.massDiff = massDiff;
  }

  public String getSeq() {
    return seq;
  }

  public void setSeq(String seq) {
    this.seq = seq;
  }

  public int getCharge() {
    return charge;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  public double getObservedNeutralMass() {
    return observedNeutralMass;
  }

  public void setObservedNeutralMass(double observedNeutralMass) {
    this.observedNeutralMass = observedNeutralMass;
  }

  public double getCalcedNeutralMass() {
    return calcedNeutralMass;
  }

  public void setCalcedNeutralMass(double calcedNeutralMass) {
    this.calcedNeutralMass = calcedNeutralMass;
  }

  @Override
  public String toString() {
    return "{" +
        "M=" + calcedNeutralMass +
        ", Mobs=" + observedNeutralMass +
        ", z=" + charge +
        ", seq='" + seq + '\'' +
        ", dM=" + String.format("%.5f", massDiff) +
        '}';

  }

  @Override
  public int compareTo(SearchHitDiff o) {
    return Double.compare(getMassDiff(), o.getMassDiff());
  }
}
