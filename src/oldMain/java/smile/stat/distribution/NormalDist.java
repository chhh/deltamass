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

package smile.stat.distribution;

//import smile.math.Math;

/**
 * @author Dmitry Avtonomov
 */
public class NormalDist extends GaussianDistribution {


  public NormalDist(double mu, double sigma) {
    super(mu, sigma);
  }

  public NormalDist(double[] data) {
    super(data);
  }

  public static MuSigma estimateGaussianParams(DataProvider x, WeightProvider w) {
    double mu = 0;
    double wSum = 0;
    for (int i = 0; i < x.size(); i++) {
      mu += x.get(i) * w.get(i);
      wSum += w.get(i);
    }
    mu = mu / wSum;

    double sigmaSq = 0;
    for (int i = 0; i < x.size(); i++) {
      double d = x.get(i) - mu;
      sigmaSq += d * d * w.get(i);
    }
    sigmaSq = sigmaSq / wSum;
    double sigma = Math.sqrt(sigmaSq);

    return new MuSigma(mu, sigma);
  }

  public Mixture.Component M(double[] x, WeightProvider w, double[] posteriori) {
    double alpha = 0.0;
    double mean = 0.0;
    double sd = 0.0;

    for (int i = 0; i < x.length; i++) {
      double wi = w == null ? 1 : w.get(i);
      alpha += posteriori[i] * wi;
      mean += x[i] * posteriori[i] * wi;
    }

    mean /= alpha;

    for (int i = 0; i < x.length; i++) {
      double wi = w == null ? 1 : w.get(i);
      double d = x[i] - mean;
      sd += (d * d * posteriori[i]) * wi;
    }

    sd = Math.sqrt(sd / alpha);

    Mixture.Component c = new Mixture.Component();
    c.priori = alpha;
    c.distribution = new NormalDist(mean, sd);

    return c;
  }

  @Override
  public String toString() {
    return String.format("NormalDist(%.4f, %.4f)", mean(), sd());
  }

  public static class MuSigma {

    public final double mu;
    public final double sigma;

    private MuSigma(double mu, double sigma) {
      this.mu = mu;
      this.sigma = sigma;
    }
  }
}
