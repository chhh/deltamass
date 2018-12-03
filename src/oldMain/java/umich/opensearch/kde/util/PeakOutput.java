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

import smile.stat.distribution.Distribution;

/**
 * @author Dmitry Avtonomov
 */
public class PeakOutput implements Comparable<PeakOutput> {

  public final double mean;
  public final double sd;
  public final double priori;
  public final Distribution distribution;

  public final double kdeTotalArea;
  public final double kdeTotalEntries;
  public final double gmmComponentArea;
  public final double gmmCombinedArea;
  public final double gmmComponentScaledHeight;
  public final double gmmSupport;

  public final double der2Loc;
  public final double der2Amplitude;
  public final double der2Value;

  protected PeakOutput(double mean, double sd, double priori, Distribution distribution,
      double kdeTotalArea, double kdeTotalEntries,
      double gmmComponentArea, double gmmCombinedArea, double gmmComponentScaledHeight,
      double gmmSupport,
      double der2Loc, double der2Amplitude, double der2Value) {
    this.mean = mean;
    this.sd = sd;
    this.priori = priori;
    this.distribution = distribution;
    this.kdeTotalArea = kdeTotalArea;
    this.kdeTotalEntries = kdeTotalEntries;
    this.gmmComponentArea = gmmComponentArea;
    this.gmmCombinedArea = gmmCombinedArea;
    this.gmmComponentScaledHeight = gmmComponentScaledHeight;
    this.gmmSupport = gmmSupport;
    this.der2Loc = der2Loc;
    this.der2Amplitude = der2Amplitude;
    this.der2Value = der2Value;
  }

  @Override
  public int compareTo(PeakOutput o) {
    return Double.compare(this.der2Loc, o.der2Loc);
  }

  public static class Builder {

    private double mean = Double.NaN;
    private double sd = Double.NaN;
    private double priori = Double.NaN;
    private Distribution distribution = null;

    private double kdeTotalArea = Double.NaN;
    private double kdeTotalEntries = Double.NaN;

    private double gmmComponentArea = Double.NaN;
    private double gmmCombinedArea = Double.NaN;
    private double gmmComponentScaledHeight = Double.NaN;
    private double gmmSupport = Double.NaN;

    private double der2Loc = Double.NaN;
    private double der2Amplitude = Double.NaN;
    private double der2Value = Double.NaN;

    public Builder setMean(double mean) {
      this.mean = mean;
      return this;
    }

    public Builder setSd(double sd) {
      this.sd = sd;
      return this;
    }

    public Builder setPriori(double priori) {
      this.priori = priori;
      return this;
    }

    public Builder setDistribution(Distribution distribution) {
      this.distribution = distribution;
      return this;
    }

    public Builder setGmmSupport(double gmmSupport) {
      this.gmmSupport = gmmSupport;
      return this;
    }

    public Builder setKdeTotalArea(double kdeTotalArea) {
      this.kdeTotalArea = kdeTotalArea;
      return this;
    }

    public Builder setKdeTotalEntries(double kdeTotalEntries) {
      this.kdeTotalEntries = kdeTotalEntries;
      return this;
    }

    public Builder setGmmComponentArea(double gmmCompArea) {
      this.gmmComponentArea = gmmCompArea;
      return this;
    }

    public Builder setGmmCombinedArea(double gmmPeakHeight) {
      this.gmmCombinedArea = gmmPeakHeight;
      return this;
    }

    public Builder setGmmComponentScaledHeight(double gmmComponentScaledHeight) {
      this.gmmComponentScaledHeight = gmmComponentScaledHeight;
      return this;
    }

    public Builder setDer2Loc(double der2Loc) {
      this.der2Loc = der2Loc;
      return this;
    }

    public Builder setDer2Amplitude(double der2Amplitude) {
      this.der2Amplitude = der2Amplitude;
      return this;
    }

    public Builder setDer2Value(double der2Value) {
      this.der2Value = der2Value;
      return this;
    }

    public PeakOutput createPeakOutput() {
      return new PeakOutput(mean, sd, priori, distribution,
          kdeTotalArea, kdeTotalEntries,
          gmmComponentArea, gmmCombinedArea, gmmComponentScaledHeight, gmmSupport,
          der2Loc, der2Amplitude, der2Value);
    }
  }
}
