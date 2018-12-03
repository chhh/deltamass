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

import java.util.ArrayList;
import java.util.List;
import umich.opensearch.kde.util.Peak;

/**
 * @author Dmitry Avtonomov
 */
public class GaussMixtureWeighted extends ExponentialFamilyMixture {

  private static final double LOG2 = java.lang.Math.log(2);
  WeightProvider w = null;
  int wSum = 0;
  double gamma;

  /**
   * Constructor. The Gaussian mixture model will be learned from the given data with the EM
   * algorithm. The number of components will be selected by BIC.
   *
   * @param data the training data.
   * @param gamma regularization parameter for EM. Should be in range [0.0-0.2].
   * @param maxIterations Max number of EM iterations. Set to Integer.MAX_VALUE if unsure.
   */
  @SuppressWarnings("unchecked")
  public GaussMixtureWeighted(final double[] data, WeightProvider w, double gamma,
      int maxIterations) {
    int wSum = 0;
    if (w != null) {
      for (int i = 0; i < data.length; i++) {
        wSum += w.get(i);
      }
      this.wSum = wSum;
    } else {
      this.wSum = data.length;
    }

    if (wSum < 20) {
      throw new IllegalArgumentException("Too few samples.");
    }

    this.w = w == null ? new WeightProvider() {
      @Override
      public int get(int i) {
        return 1;
      }
    } : w;
    this.gamma = gamma;

    ArrayList<Component> mixture = new ArrayList<>();
    Component c = new Component();
    c.priori = 1.0;
    DataProvider dp = new DataProvider() {
      @Override
      public double get(int i) {
        return data[i];
      }

      @Override
      public int size() {
        return data.length;
      }
    };
    NormalDist.MuSigma muSigma = NormalDist.estimateGaussianParams(dp, w);
    c.distribution = new NormalDist(muSigma.mu, muSigma.sigma);
    mixture.add(c);

    int freedom = 0;
    for (Component aMixture : mixture) {
      freedom += aMixture.distribution.npara();
    }

    double bic = 0.0;
    for (int i = 0; i < data.length; i++) {
      double xi = data[i];
      double wi = w == null ? 1 : w.get(i);
      if (wi <= 0) {
        continue;
      }
      double p = c.distribution.p(xi);
      if (p > 0) {
        bic += Math.log(p) * wi;
      }
    }
    bic -= 0.5 * freedom * Math.log(wSum);

    double b = Double.NEGATIVE_INFINITY;

    while (bic > b) {
      b = bic;
      components = (ArrayList<Component>) mixture.clone();

      split(mixture);
      bic = EM(mixture, data, gamma, maxIterations);

      freedom = 0;
      for (int i = 0; i < mixture.size(); i++) {
        freedom += mixture.get(i).distribution.npara();
      }
      bic -= 0.5 * freedom * Math.log(wSum);
    }
  }


  /**
   * Optimize an exising GMM using EM algorithm.
   *
   * @param initMix The initial GMM to be optimized.
   * @param doSingleEMround If true, will only run EM once, thus no new components will be added to
   * GMM, but only the existing ones' means and sigmas updated.
   * @param gamma Regularization parameter. Should generally be zero, unless runtimes are too slow
   * for you.
   * @param maxIterations Max number of iterations of EM till convergence. Reasonable number is
   * 100-1000.
   */
  @SuppressWarnings("unchecked")
  public GaussMixtureWeighted(ArrayList<MixtureComponentPeak> initMix, boolean doSingleEMround,
      double[] data, WeightProvider w, double gamma, int maxIterations) {
    int wSum = 0;
    if (w != null) {
      for (int i = 0; i < data.length; i++) {
        wSum += w.get(i);
      }
      this.wSum = wSum;
    } else {
      this.wSum = data.length;
    }

    if (wSum < 20) {
      throw new IllegalArgumentException("Too few samples.");
    }

    this.w = w == null ? new WeightProvider() {
      @Override
      public int get(int i) {
        return 1;
      }
    } : w;
    this.gamma = gamma;

    ArrayList<Component> mixture = (ArrayList<Component>) initMix.clone();
    components = mixture;

    double sumCompWeights = 0;
    for (Component component : mixture) {
      sumCompWeights += component.priori;
    }
    for (Component component : mixture) {
      component.priori /= sumCompWeights;
    }
    int freedom = 0;
    for (Component aMixture : mixture) {
      freedom += aMixture.distribution.npara();
    }

    double bic = EM(mixture, data, gamma, maxIterations);
    double b = Double.NEGATIVE_INFINITY;

    if (!doSingleEMround) {
      while (bic > b) {
        b = bic;
        components = (ArrayList<Component>) mixture.clone();

        split(mixture);
        bic = EM(mixture, data, gamma, maxIterations);

        freedom = 0;
        for (int i = 0; i < mixture.size(); i++) {
          freedom += mixture.get(i).distribution.npara();
        }
        bic -= 0.5 * freedom * Math.log(wSum);
      }
    }
  }

  private static double log2(double x) {
    return java.lang.Math.log(x) / LOG2;
  }

  /**
   * Split the most heterogeneous cluster along its main direction (eigenvector).
   */
  private void split(List<Component> mixture) {
    // Find most dispersive cluster (biggest sigma)
    Component componentToSplit = null;

    double maxSigma = 0.0;
    for (Component c : mixture) {
      if (c.distribution.sd() > maxSigma) {
        maxSigma = c.distribution.sd();
        componentToSplit = c;
      }
    }

    // Splits the component
    if (componentToSplit == null) {
      return;
    }
    double delta = componentToSplit.distribution.sd();
    double mu = componentToSplit.distribution.mean();
    Peak p = null;
    if (componentToSplit instanceof MixtureComponentPeak) {
      MixtureComponentPeak mc = (MixtureComponentPeak) componentToSplit;
      p = mc.peak;
    }

    MixtureComponentPeak c = new MixtureComponentPeak(p);
    c.priori = componentToSplit.priori / 2;
    c.distribution = new NormalDist(mu + delta / 2, delta);
    mixture.add(c);

    c = new MixtureComponentPeak(p);
    c.priori = componentToSplit.priori / 2;
    c.distribution = new NormalDist(mu - delta / 2, delta);
    mixture.add(c);

    mixture.remove(componentToSplit);
  }

  @Override
  double EM(List<Component> components, double[] x, double gamma, int maxIter) {
    if (wSum < components.size() / 2) {
      throw new IllegalArgumentException("Too many components");
    }

    if (gamma < 0.0 || gamma > 0.2) {
      throw new IllegalArgumentException("Invalid regularization factor gamma.");
    }

    int n = x.length;
    int m = components.size();

    double[][] posteriori = new double[m][n];

    // Log Likelihood
    double L = 0.0;
    for (int i = 0; i < x.length; i++) {
      double xi = x[i];
      double wi = w == null ? 1 : w.get(i);
      double p = 0.0;
      for (Component c : components) {
        p += c.priori * c.distribution.p(xi);
      }
      if (p > 0) {
        L += Math.log(p) * wi;
      }
    }

    // EM loop until convergence
    int iter = 0;
    for (; iter < maxIter; iter++) {

      // Expectation step
      for (int i = 0; i < m; i++) {
        Component c = components.get(i);

        for (int j = 0; j < n; j++) {
          posteriori[i][j] = c.priori * c.distribution.p(x[j]);
        }
      }

      // Normalize posteriori probability.
      for (int j = 0; j < n; j++) {
        double p = 0.0;

        for (int i = 0; i < m; i++) {
          p += posteriori[i][j];
        }

        for (int i = 0; i < m; i++) {
          posteriori[i][j] /= p;
        }

        // Adjust posterior probabilites based on Regularized EM algorithm.
        if (gamma > 0) {
          for (int i = 0; i < m; i++) {
            posteriori[i][j] *= (1 + gamma * log2(posteriori[i][j]));
            if (Double.isNaN(posteriori[i][j]) || posteriori[i][j] < 0.0) {
              posteriori[i][j] = 0.0;
            }
          }
        }
      }

      // Maximization step
      ArrayList<Component> newConfig = new ArrayList<>();
      for (int i = 0; i < m; i++) {
        final Component c = components.get(i);
        MixtureComponentPeak mc = null;
        if (c instanceof MixtureComponentPeak) {
          mc = (MixtureComponentPeak) c;
        }
        Distribution distribution = c.distribution;
        if (w == null) {
          final Component m1 = ((ExponentialFamily) distribution).M(x, posteriori[i]);
          newConfig.add(mc == null ? m1 : MixtureComponentPeak.from(m1, mc.peak));
        } else if (distribution instanceof NormalDist) {
          NormalDist normalDist = (NormalDist) distribution;
          final Component m1 = normalDist.M(x, w, posteriori[i]);
          newConfig.add(mc == null ? m1 : MixtureComponentPeak.from(m1, mc.peak));
        } else {
          throw new IllegalStateException(
              "Only NormalDist supported when weight are provided to GaussMixtureWeighted");
        }
      }

      double sumAlpha = 0.0;
      for (int i = 0; i < m; i++) {
        sumAlpha += newConfig.get(i).priori;
      }

      for (int i = 0; i < m; i++) {
        newConfig.get(i).priori /= sumAlpha;
      }

      double newL = 0.0;
      for (int i = 0; i < x.length; i++) {
        double xi = x[i];
        double wi = w == null ? 1 : w.get(i);
        double p = 0.0;
        for (Component c : newConfig) {
          p += c.priori * c.distribution.p(xi);
        }
        if (p > 0) {
          newL += Math.log(p) * wi;
        }
      }

      if (newL > L) {
        L = newL;
        components.clear();
        components.addAll(newConfig);
      } else {
        break;
      }
    }

    return L;
  }
}
