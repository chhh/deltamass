package umich.opensearch.kde.util;

import com.github.chhh.utils.LogUtils;
import hep.aida.tdouble.bin.MightyStaticDoubleBin1D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jsat.linear.DenseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.math.DoubleArrayList;
import smile.math.IntArrayList;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import umich.opensearch.kde.OpenSearchParams;
import umich.opensearch.kde.api.IWeightedValues;
import umich.opensearch.kde.params.denoise.Denoiser;
import umich.opensearch.kde.params.denoise.Denoising;
import umich.opensearch.kde.params.denoise.NumberedParams;
import umich.opensearch.kde.params.denoise.TotalVariationDenoiser;

/**
 * @author Dmitry Avtonomov
 */
public class PeakUtils {

  private static final Logger log = LoggerFactory.getLogger(PeakUtils.class);

  private PeakUtils() {
  }


  public static List<Maximum> findMaxima(double[] dataX, double[] dataY) {
    ArrayList<Maximum> maxima = new ArrayList<>(64);
    double valPre = Double.POSITIVE_INFINITY;
    double valCur;
    int possibleMaxPlateuStartIdx = -1;
    int possibleMaxPlateuEndIdx = -1;
    for (int i = 0; i < dataY.length; i++) {
      valCur = dataY[i];
      if (valCur > valPre) { // climbing up
        possibleMaxPlateuStartIdx = i;
        possibleMaxPlateuEndIdx = i;
      } else if (valCur == valPre) { // on a high plateau
        possibleMaxPlateuEndIdx = i;
      } else if (valCur < valPre) { // found a peak/high plateau
        if (possibleMaxPlateuStartIdx > 0 && possibleMaxPlateuEndIdx > 0) {
          maxima.add(new Maximum(
              possibleMaxPlateuStartIdx, possibleMaxPlateuEndIdx,
              dataX[possibleMaxPlateuStartIdx], dataX[possibleMaxPlateuEndIdx],
              dataY[possibleMaxPlateuStartIdx]));
        }
        possibleMaxPlateuEndIdx = -1;
        possibleMaxPlateuStartIdx = -1;
      }
      valPre = valCur;
    }
    return maxima;
  }

  public static NoiseResult estimateNoiseDistribution(OpenSearchParams params,
      final IndexBracket brX,
      final double[] xAxis, final double[] yAxis, DenseVector vecLocal) {
    IndexBracket brVec = BracketUtils.findBracket(brX.loVal, brX.hiVal, vecLocal);

    // non-zero areas of psm density function (think non-zero areas of KDE plot)
    List<IndexBracket> locations = BracketUtils.findNonZeroBrackets(xAxis, yAxis, brX.lo, brX.hi);

    int entriesUnderKde = locations.stream()
        .map(kdeBracket -> BracketUtils.findBracket(kdeBracket.loVal, kdeBracket.hiVal, vecLocal))
        .mapToInt(vecLocalBracket -> vecLocalBracket == null ? 0 : vecLocalBracket.size)
        .sum();

    // TODO: ACHTUNG: Continue optimization path here
    final double[] xVals = BracketUtils.createArraySliceCopy(brX, xAxis);
    final double[] yVals = BracketUtils.createArraySliceCopy(brX, yAxis);

    IWeightedValues data = new IWeightedValues() {
      @Override
      public int size() {
        return brX.size;
      }

      @Override
      public double val(int i) {
        return xAxis[brX.lo + i];
      }

      @Override
      public double weight(int i) {
        return yAxis[brX.lo + i];
      }
    };

    final double yValsOrigSum = Arrays.stream(yVals).sum();
    for (int i = 0; i < yVals.length; i++) {
      yVals[i] /= yValsOrigSum;
    }
    final double xStep = xVals[1] - xVals[0];

    InitNoiseMixEstimate init = new InitNoiseMixEstimate(brX, xVals, yVals).invoke();

    List<MixCompUni> mix = new ArrayList<>();
    mix.add(new CompNorm(init.d1w, init.d1mu, init.d1sd));
    mix.add(new CompNorm(init.d2w, init.d2mu, init.d2sd));

    double[][] expect = createExpectArray(mix, data);
    for (int i = 0; i < 5; i++) {
      int a = 1;
      E(mix, data, expect);
//            normalizeExpectArray(expect);
      M(mix, data, expect);
      normalizeMixWeights(mix);
      int b = 1;
    }

    return null;
  }

  private static double[][] createExpectArray(List<MixCompUni> mix, IWeightedValues data) {
    return new double[mix.size()][data.size()];
  }

  private static void normalizeExpectArray(double[][] expect) {
    int size = expect[0].length;
    for (int i = 0; i < size; i++) {
      double sum = 0;
      for (double[] e : expect) {
        sum += e[i];
      }
      for (double[] e : expect) {
        e[i] /= sum;
      }
    }
  }

  private static void normalizeMixWeights(List<MixCompUni> mix) {
    double sum = 0;
    for (MixCompUni comp : mix) {
      sum += comp.w();
    }
    for (MixCompUni comp : mix) {
      comp.updateWeight(comp.w() / sum);
    }
  }

  private static void E(List<MixCompUni> mix, IWeightedValues data, final double[][] expect) {
    if (expect.length < mix.size()) {
      throw new IllegalArgumentException(
          "first dim of expect array was smaller than the number of mixture components");
    }
    for (int i = 0; i < expect.length; i++) {
      if (expect[i].length < data.size()) {
        throw new IllegalArgumentException(
            String.format("expect array dimension [%d] was smaller than data size", i));
      }
    }
    for (int iMix = 0; iMix < mix.size(); iMix++) {
      MixCompUni comp = mix.get(iMix);
      for (int iData = 0; iData < data.size(); iData++) {
        double v = comp.w() * comp.p(data.val(iData));
        if (!Double.isFinite(v) || Double.isNaN(v)) {
          int a = 1;
        }
        expect[iMix][iData] = v;
      }
    }
  }

  private static void M(List<MixCompUni> mix, IWeightedValues data, final double[][] expect) {
    if (expect.length < mix.size()) {
      throw new IllegalArgumentException(
          "first dim of expect array was smaller than the number of mixture components");
    }
    for (int i = 0; i < expect.length; i++) {
      if (expect[i].length < data.size()) {
        throw new IllegalArgumentException(
            String.format("expect array dimension [%d] was smaller than data size", i));
      }
    }
    for (int i = 0; i < mix.size(); i++) {
      mix.get(i).M(data, expect[i]);
    }
  }

  public static BracketResult findPeaksGMM4(OpenSearchParams params, IndexBracket brX,
      double[] xAxis, double[] yAxis, double[] yDer2, DenseVector vecLocal) {
    List<Peak> peaks = new ArrayList<>();
    BracketResult result = new BracketResult();
    result.peaks = peaks;
    result.bracket = brX;

    if (vecLocal.length() < params.getGmmMinSupport()) {
      log.info(String.format("Fewer than %d data points, not searching for peaks.",
          params.getGmmMinSupport()));
      return result;
    }

    // find the number of entries in the non-zero portion of yAxis
    List<IndexBracket> nonZeroBrackets = BracketUtils
        .findNonZeroBrackets(xAxis, yAxis, brX.lo, brX.hi);
    int cntNonZeroDensityEntries = 0;
    for (IndexBracket nonZeroBracket : nonZeroBrackets) {
      IndexBracket bracket = BracketUtils
          .findBracket(nonZeroBracket.loVal, nonZeroBracket.hiVal, vecLocal);
      cntNonZeroDensityEntries += bracket.size;
    }
    result.numEntries = cntNonZeroDensityEntries;
    if (cntNonZeroDensityEntries < params.getGmmMinSupport()) {
      DecimalFormat df2 = new DecimalFormat("0.00");
      log.info(
          "Fewer than {} data points in non-zero density regions of [{}-{}], not searching for peaks.",
          params.getGmmMinSupport(), df2.format(brX.loVal), df2.format(brX.hiVal));
      return result;
    }

    // parameters for detecting local minima in second derivative
    PeakDetectionOptions optsPeaksDer = new PeakDetectionOptions();
    optsPeaksDer.peakAcceptor = p -> p.numNonZeroPts > 2;
    optsPeaksDer.zeroTolerance = 1e-8;

    // copy the 2nd derivative and invert the sign, so that we could use maxima finding routine
    double[] yDer2InvSign = BracketUtils.createArraySliceCopy(brX, yDer2);
    for (int i = 0; i < yDer2InvSign.length; i++) {
      yDer2InvSign[i] *= -1;
    }

    // local minima in the 2nd derivative
    List<PeakApprox> peaksDer = detectPeakLocations(yDer2InvSign, optsPeaksDer);
    // remove flanking plateaus and invert the sign back
    peaksDer.removeIf(p -> p.valTop == 0.0);

    // fit them with parabolas
    List<Peak> peaksDerFitted = new ArrayList<>(peaksDer.size());
    for (PeakApprox p : peaksDer) {
      Peak parabolicPeak = createParabolicPeak(brX.lo, true, xAxis, yDer2, p);
      peaksDerFitted.add(parabolicPeak);
    }
    result.peaks.clear();
    result.peaks.addAll(peaksDerFitted);

    int gmmNumEntries = params.getGmmNumEntries();
    int gmmMinEntries = params.getGmmMinEntries();
    final WeightedFakePoints fakePoints = createFakeGmmPointsWeighted(brX, xAxis, yAxis,
        gmmNumEntries, gmmMinEntries);
    if (fakePoints.weightSum < 20) {
      return result;
    }
    //WeightProvider w = i -> fakePoints.weights[i];
    WeightProvider w = i -> fakePoints.weights[i];

    final double gamma = 0.0;
    final int maxIters = 5;

    // if we have some assumptions about peak locations, try fitting GMM
    if (!result.peaks.isEmpty()) {
      ArrayList<MixtureComponentPeak> initMix = new ArrayList<>();
      for (Peak p : result.peaks) {
        double mu = p.x;
        MixtureComponentPeak c = new MixtureComponentPeak(p);
        double sd;
        switch (p.widthType) {
          case SD:
            sd = p.width;
            break;
          case FWHM:
            sd = p.width / 2.3548;
            break;
          case FULL:
            sd = p.width / 4;
            break;
          default:
            throw new UnsupportedOperationException("Standard deviation calc for peak with type " +
                p.widthType.toString() + " is not supported.");
        }

        c.priori = p.y;
        c.distribution = new NormalDist(mu, sd);
        initMix.add(c);
      }

      // make sure that priors add up to 1
      double sum = 0;
      for (Mixture.Component c : initMix) {
        sum += c.priori;
      }
      for (Mixture.Component c : initMix) {
        c.priori /= sum;
      }

      // TODO: maybe MixtureComponentPeak should contain all the peak info, not only the initial MU

      GaussMixtureWeighted gmw = new GaussMixtureWeighted(initMix, true, fakePoints.values, w,
          gamma, maxIters);

      // TODO: continue here, now the initial component's MUs are stored in the reesulting components after GMM fitting

      GaussianMixture gm = new GaussianMixture(gmw.getComponents());

      // sanity check GMM results
      GaussianMixture gmChecked = sanityCheckGmm(params, brX, xAxis, vecLocal, gm);
      result.gmm = gmChecked;
    }

    return result;
  }

  private static Peak createParabolicPeak(double[] xAxis, double[] yAxis, PeakApprox peakApprox) {
    return createParabolicPeak(0, false, xAxis, yAxis, peakApprox);
  }

  private static Peak createParabolicPeak(int idxShift, boolean peakYrangeAsHeight, double[] xAxis,
      double[] yAxis, PeakApprox peakApprox) {

    double vertexX;
    if (peakApprox.idxTopHi == peakApprox.idxTopLo) {
      int idx = peakApprox.idxTopLo;
      if (idx > 0 && idx < xAxis.length - 1) {
        double[] x = new double[3];
        double[] y = new double[3];
        for (int i = -1; i <= 1; i++) {
          x[i + 1] = xAxis[idxShift + idx + i];
          y[i + 1] = yAxis[idxShift + idx + i];
        }
        double[] parabola = fitParabola(x, y);
        vertexX = parabolaVertexX(parabola[2], parabola[1]);
      } else {
        vertexX = xAxis[idxShift + peakApprox.idxTopLo];
      }

    } else {
      int idx = peakApprox.idxTopLo;
      if (idx > 0) {
        double[] x = new double[3];
        double[] y = new double[3];
        x[0] = xAxis[idxShift + idx - 1];
        y[0] = yAxis[idxShift + idx - 1];
        x[1] = xAxis[idxShift + idx];
        y[1] = yAxis[idxShift + idx];
        x[2] = xAxis[idxShift + peakApprox.idxTopHi];
        y[2] = yAxis[idxShift + peakApprox.idxTopHi];

        double[] parabola = fitParabola(x, y);
        vertexX = parabolaVertexX(parabola[2], parabola[1]);
      } else {
        vertexX = xAxis[idxShift + peakApprox.idxTopLo];
      }

    }

    double height;
    if (!peakYrangeAsHeight) {
      height = peakApprox.valTop;
    } else {
      height = peakApprox.valTop - Math.max(peakApprox.valLo, peakApprox.valHi);
    }

    double width = xAxis[idxShift + peakApprox.idxHi] - xAxis[idxShift + peakApprox.idxLo];

    Peak p = new Peak(peakApprox, vertexX, height, width, Peak.WIDTH.FULL);
    return p;
  }

  /**
   * Checks that GMM does not have too narrow components or components with too weak evidence
   * support.
   *
   * @param gm Will be modified in-place.
   */
  private static GaussianMixture sanityCheckGmm(OpenSearchParams params, IndexBracket brX,
      double[] xAxis, DenseVector vecLocal, GaussianMixture gm) {
    List<Mixture.Component> toRemove = new ArrayList<>();
    int entriesInBracket = vecLocal.length();
    double step = xAxis[1] - xAxis[0];
    for (Mixture.Component c1 : gm.getComponents()) {
      Mixture.Component componentToRemove = null;
      if (c1.distribution.sd() * 6 < step) {
        componentToRemove = c1;
        //LogHelper.logMsg(String.format(
        //        "[%.2f-%.2f] Encountered GMM component with very small SD. Mu=%.2f, Sigma=%.2f, removing.",
        //        brX.loVal, brX.hiVal, c1.distribution.mean(), c1.distribution.sd()), out, logger, true);
      }
      int minCnt = params.getGmmMinSupport();
      if (c1.priori * entriesInBracket < minCnt) {
        componentToRemove = c1;
        //LogHelper.logMsg(String.format(
        //        "[%.2f-%.2f] Encountered GMM component that is supported by less than %d entries. Mu=%.2f, Sigma=%.2f, removing.",
        //        brX.loVal, brX.hiVal, minCnt, c1.distribution.mean(), c1.distribution.sd()), out, logger, true);
      } else {
        // a more stringent check, validate that in 4*sd there were at least N data points
        double loTarget = c1.distribution.mean() - c1.distribution.sd() * 2;
        double hiTarget = c1.distribution.mean() + c1.distribution.sd() * 2;
        IndexBracket b = BracketUtils.findBracket(loTarget, hiTarget, vecLocal);
        if (b.size < minCnt) {
          componentToRemove = c1;
          //LogHelper.logMsg(String.format(
          //        "[%.2f-%.2f] Encountered GMM component that is supported by less than %d entries. Mu=%.2f, Sigma=%.2f, removing.",
          //        brX.loVal, brX.hiVal, minCnt, c1.distribution.mean(), c1.distribution.sd()), out, logger, true);
        }
      }
      if (componentToRemove != null && !toRemove.contains(componentToRemove)) {
        toRemove.add(componentToRemove);
      }
    }
    for (Mixture.Component c : toRemove) {
      gm.getComponents().remove(c);
    }

    if (gm.getComponents().isEmpty()) {
      return null;
    }

    // renormalize
    double sum = 0;
    for (Mixture.Component c : gm.getComponents()) {
      sum += c.priori;
    }
    for (Mixture.Component c : gm.getComponents()) {
      c.priori = c.priori / sum;
    }

    return gm;
  }

  /**
   * 3-point fit of a parabola.
   *
   * @param x the x coordinates.
   * @param y the y coordinates.
   * @return double[]: [0] - c, [1] - b, [2] - a
   */
  public static double[] fitParabola(double[] x, double[] y) {
    if (x.length != 3) {
      throw new IllegalArgumentException("Length of input data arrays must be 3.");
    }
    if (x.length != y.length) {
      throw new IllegalArgumentException("Arrays x and y must be of the same length: 3.");
    }
    double a = (y[1] * (x[2] - x[0]) - y[0] * (x[2] - x[1]) - y[2] * (x[1] - x[0])) / (
        Math.pow(x[0], 2) * (x[1] - x[2]) - Math.pow(x[2], 2) * (x[1] - x[0])
            - Math.pow(x[1], 2) * (x[0] - x[2]));
    double b = (y[1] - y[0] + a * (Math.pow(x[0], 2) - Math.pow(x[1], 2))) / (x[1] - x[0]);
    double c = -1 * a * Math.pow(x[0], 2) - b * x[0] + y[0];
    double[] parabola = new double[3];
    parabola[0] = c;
    parabola[1] = b;
    parabola[2] = a;
    return parabola;
  }

  public static double parabolaVertexX(double a, double b) {
    return -1 * b / (2.0 * a);
  }

  private static WeightedFakePoints createFakeGmmPointsWeighted(IndexBracket brX, double[] xAxis,
      double[] yAxis, int totalPoints, int minCnt) {
    final int minPts = 20;
    if (totalPoints < minPts) {
      throw new IllegalArgumentException(
          String.format("The minimum is %d points to be spread out across", minPts));
    }
    double step = xAxis[1] - xAxis[0];
    double area = 0;
    for (int i = brX.lo; i < brX.hi; i++) {
      area += yAxis[i] * step;
    }
    double density = totalPoints / area;

    double accumArea = 0;
    double sumX = 0;
    double sumY = 0;
    DoubleArrayList points = new DoubleArrayList(totalPoints);
    IntArrayList weights = new IntArrayList(totalPoints);

    int weightSum = 0;
    for (int i = brX.lo; i < brX.hi; i++) {
      accumArea += yAxis[i] * step;
      sumX += xAxis[i] * yAxis[i];
      sumY += yAxis[i];
      double accumCount = accumArea * density;
      if (accumCount > 1) {
        double val = sumX / sumY;
        int cnt = (int) Math.floor(accumCount);
        if (cnt >= 1) {
          points.add(val);
          weights.add(cnt);
          weightSum += cnt;
        }
        accumArea = 0;
        sumX = 0;
        sumY = 0;
      }
    }
    return new WeightedFakePoints(points.toArray(), weights.toArray(), weightSum);
  }

  public static List<Peak> findPeaks(Appendable out, OpenSearchParams params,
      double[] xAxis, double[] yAxis) {
    double[] denoised;
    if (params.getDenoising() == Denoising.NONE) {
      denoised = Arrays.copyOf(yAxis, yAxis.length);
    } else {
      denoised = new double[yAxis.length];
      Denoiser<? extends NumberedParams> denoiser = params.getDenoising().getInstance();
      NumberedParams denoiserProperties = denoiser.getDefaultConfig();
      if (!params.getDenoisingParams().isEmpty()) {
        denoiserProperties = denoiser.configure(params.getDenoisingParams());
      }
      denoiserProperties.put("massAxis", xAxis);
      denoiserProperties.put(TotalVariationDenoiser.Config.PROP_DO_PLOT,
          String.valueOf(params.isDenoisingPlot()));
      denoiser.denoise(yAxis, denoised, denoiserProperties);
    }
    List<Maximum> maxima = findMaxima(xAxis, denoised);

    List<Peak> peaks = new ArrayList<>(maxima.size());
    if (maxima.size() > 0) {

      MightyStaticDoubleBin1D binWi = new MightyStaticDoubleBin1D(true, true, 4);
      MightyStaticDoubleBin1D binHi = new MightyStaticDoubleBin1D(true, true, 4);

      for (Maximum maximum : maxima) {
        double width = Math.abs(maximum.xHi - maximum.xLo);
        binWi.add(width);
        binHi.add(maximum.val);
      }

      double stddevWi = 0;
      double stddevHi = 0;
      double meanWi = binWi.mean();
      double meanHi = binHi.mean();

      for (Maximum maximum : maxima) {
        stddevWi += Math.pow(Math.abs((maximum.xHi - maximum.xLo)) - meanWi, 2d);
        stddevHi += Math.pow(maximum.val - meanHi, 2d);
      }
      stddevWi = Math.sqrt(stddevWi);
      stddevHi = Math.sqrt(stddevHi);

      DecimalFormat fmt = new DecimalFormat("0.###E0");

      if (out != null) {
        String msg = String.format("Number of detected peaks: %d", maxima.size());
        LogUtils.println(out, msg);
        msg = String.format(
            "Peak width stats: min=%s, max=%.4f, avg=%.4f, stddev=%.4f, harmonic=%s, geometric=%s",
            fmt.format(binWi.min()), binWi.max(), binWi.mean(), stddevWi,
            fmt.format(binWi.harmonicMean()), fmt.format(binWi.geometricMean()));
        LogUtils.println(out, msg);
        msg = String.format(
            "Peak height stats: min=%s, max=%.4f, avg=%.4f, stddev=%.4f, harmonic=%s, geometric=%s",
            fmt.format(binHi.min()), binHi.max(), binHi.mean(), stddevHi,
            fmt.format(binHi.harmonicMean()), fmt.format(binHi.geometricMean()));
        LogUtils.println(out, msg);
        LogUtils.println(out, "");
      }

      for (Maximum maximum : maxima) {
        double minAcceptedPeakWidth = binWi.mean() + stddevWi;
        double peakWidth = (maximum.xHi - maximum.xLo);
        if (peakWidth <= minAcceptedPeakWidth) {
          double avgPeakX = (maximum.xLo + maximum.xHi) / 2d;
          double peakHeight;
          int idxSum = maximum.idxLo + maximum.idxHi;
          if (maximum.idxLo == maximum.idxHi) {
            peakHeight = yAxis[maximum.idxLo];
          } else if (idxSum % 2 != 0) {
            // the sum of indexes is odd
            // we will take the average of the 2 central cells
            int idxLo = (maximum.idxLo + maximum.idxHi) / 2;
            peakHeight = (yAxis[idxLo] + yAxis[idxLo + 1]) / 2d;
          } else {
            // the sum of indexes is even, there is one single central element
            int idxCenter = (maximum.idxLo + maximum.idxHi) / 2;
            peakHeight = yAxis[idxCenter];
          }
          peaks.add(new Peak(avgPeakX, peakHeight));
        } else {
          //System.err.println("PEAK HAS BEEN REJECTED!");
        }
      }
    }
    return peaks;
  }

  private static List<PeakApprox> detectPeakLocations(double[] data, PeakDetectionOptions opts) {
    return detectPeakLocations(data, 0, data.length, opts);
  }

  /**
   * Finds locations of maxima. Can detect plateaus.
   *
   * @param data Array with data values to detect peaks in.
   * @param from Inclusive.
   * @param to Exclusive.
   * @param opts Basic peak detection options.
   */
  private static List<PeakApprox> detectPeakLocations(double[] data, int from, int to,
      PeakDetectionOptions opts) {
    if (from < 0 || from > data.length - 1) {
      throw new IllegalArgumentException("'from' must be within data[] index range");
    }
    if (to < from) {
      throw new IllegalArgumentException("'to' must be >= 'from'");
    }
    if (data.length < 3) {
      return Collections.emptyList();
    }

    List<PeakApprox> peaks = new ArrayList<>(Math.min(data.length / 10, 64));
    double diff;
    PeakApprox p = null;
    State s0 = State.FLAT, s1;

    int lastFlatStart = -1;
    for (int i = from; i < to - 1;
        i++) // -1 because to is exclusive and we're comparing to i+1 data point at each step
    {
      int ip1 = i + 1;
      diff = data[ip1] - data[i];
      if (diff > 0) {
        s1 = State.UP;
      } else if (diff < 0) {
        s1 = State.DOWN;
      } else {
        s1 = State.FLAT;
      }
      if (s1 == State.FLAT && s0 != State.FLAT) {
        lastFlatStart = i;
      }

      switch (s1) {
        case UP:
          if (p == null) {
            p = peakStart(i);
          } else if (p.idxHi != p.idxTopHi) {
            peakFinish(data, opts, peaks, p, lastFlatStart);
            p = peakStart(i);
          }
          p.idxTopLo = ip1;
          p.idxTopHi = ip1;
          p.idxHi = ip1;
          break;

        case FLAT:
          if (p == null) {
            break;
          }
          if (p.idxTopHi == p.idxHi) {
            p.idxTopHi = ip1;
          }
          p.idxHi = ip1;
          break;

        case DOWN:
          if (p == null) {
            break;
          }
          p.idxHi = ip1;
          break;
      }

      s0 = s1;
    }
    if (p != null) {
      peakFinish(data, opts, peaks, p, lastFlatStart);
    }

    return peaks;
  }

  /**
   * Starts a new peak and initializes its {@code idxLo} and {@code valLo} fields.
   *
   * @param i Current pointer in the data array.
   * @return A new peak object.
   */
  private static PeakApprox peakStart(int i) {
    PeakApprox p = new PeakApprox();
    p.idxLo = i;

    return p;
  }

  /**
   * Add the peak to a list if it passes certain criteria.
   *
   * @param data The data array.
   * @param opts Options for peak detection.
   * @param toAddTo The list to add the peak to if it passes quality checks.
   * @param p The peak to be finalized.
   * @param lastFlatStart The index where the last flat area started. If it's between {@code
   * idxTopHi} and {@code idxHi}, it will be used instead of {@code idxHi}
   * @return True, if the peak passed criteria in {@code logic} and was added to {@code toAddTo}.
   */
  private static boolean peakFinish(double[] data, PeakDetectionOptions opts,
      List<PeakApprox> toAddTo, PeakApprox p, int lastFlatStart) {
    boolean wasAdded = false;
    if (lastFlatStart > p.idxTopHi && lastFlatStart < p.idxHi) {
      p.idxHi = lastFlatStart;
    }
    p.valLo = data[p.idxLo];

    p.valTop = data[p.idxTopLo];
    p.valHi = data[p.idxHi];
    p.numNonZeroPts = p.idxHi - p.idxLo + 1;
    if (data[p.idxLo] == 0) {
      p.numNonZeroPts--;
    }
    if (data[p.idxHi] == 0) {
      p.numNonZeroPts--;
    }
    if (opts.peakAcceptor.test(p)) {
      toAddTo.add(p);
      wasAdded = true;
    }
    return wasAdded;
  }

  enum State {UP, DOWN, FLAT}

  private interface MixCompUni {

    double w();

    double p(double x);

    void updateWeight(double w);

    void M(IWeightedValues data, double[] posterior);
  }

  public static class NoiseResult {

  }

  private static class CompNorm implements MixCompUni {

    public GaussianDistribution d;
    public double w;

    public CompNorm(double w, double mu, double sd) {
      this.w = w;
      this.d = new GaussianDistribution(mu, sd);
    }

    @Override
    public String toString() {
      return String.format("Normal distribution (mu=%.4f, sd=%.4f), w=%.4f", d.mean(), d.sd(), w);
    }

    @Override
    public double w() {
      return w;
    }

    @Override
    public double p(double x) {
      return d.p(x);
    }

    @Override
    public void updateWeight(double w) {
      this.w = w;
    }

    @Override
    public void M(IWeightedValues data, double[] posterior) {
      if (posterior.length < data.size()) {
        throw new IllegalArgumentException("posterior array was shorter than data length");
      }

      double posteriorSum = 0;
      double wSum = 0;
      double muSum = 0;
      for (int i = 0; i < data.size(); i++) {
        wSum += data.weight(i);
        posteriorSum += posterior[i];
        muSum += data.val(i) * data.weight(i) * posterior[i];
      }
      final double mu = muSum / wSum;

      // variance for weighted samples, follow:
      // https://stats.stackexchange.com/questions/47325/bias-correction-in-weighted-variance
      double varianceSum = 0;
      double w2Sum = 0;
      for (int i = 0; i < data.size(); i++) {
        double d = data.val(i) - mu;
        varianceSum += d * d * posterior[i];
        w2Sum += data.weight(i) * data.weight(i);
      }
      double varianceNonWeighted = varianceSum / data.size();
      final double variance = varianceNonWeighted * (wSum * wSum) / (wSum * wSum - w2Sum * w2Sum);
      final double sd = Math.sqrt(variance);

      w = posteriorSum;
      d = new GaussianDistribution(mu, sd);
    }
  }

  private static class CompUniform implements MixCompUni {

    public double w;
    public double p;

    @Override
    public double w() {
      return w;
    }

    @Override
    public double p(double x) {
      return p;
    }

    @Override
    public void updateWeight(double w) {
      this.w = w;
    }

    @Override
    public void M(IWeightedValues data, double[] posterior) {
      throw new NotImplementedException();
    }
  }

  public static class BracketResult {

    public IndexBracket bracket;
    public List<Peak> peaks = new ArrayList<>();
    public List<PeakOutput> peaksOutput = new ArrayList<>();
    public GaussianMixture gmm;
    public int numEntries;
  }

  private static class WeightedFakePoints {

    final double[] values;
    final int[] weights;
    final int weightSum;

    private WeightedFakePoints(double[] values, int[] weights, int weightSum) {
      this.values = values;
      this.weights = weights;
      this.weightSum = weightSum;
    }
  }

  private static class PeakDetectionOptions {

    Predicate<PeakApprox> peakAcceptor = peakApprox -> true;
    double zeroTolerance = 1e-8;
  }

  private static class InitNoiseMixEstimate {

    private IndexBracket brX;
    private double[] xVals;
    private double[] yVals;
    private double d1mu;
    private double d2mu;
    private double d1sd;
    private double d2sd;
    private double d1w;
    private double d2w;

    public InitNoiseMixEstimate(IndexBracket brX, double[] xVals, double... yVals) {
      this.brX = brX;
      this.xVals = xVals;
      this.yVals = yVals;
    }

    public double getD1mu() {
      return d1mu;
    }

    public double getD2mu() {
      return d2mu;
    }

    public double getD1sd() {
      return d1sd;
    }

    public double getD2sd() {
      return d2sd;
    }

    public double getD1w() {
      return d1w;
    }

    public double getD2w() {
      return d2w;
    }

    public InitNoiseMixEstimate invoke() {
      // mu of the 'narrow' main distribution
      PeakDetectionOptions optsPeaksDer = new PeakDetectionOptions();
      optsPeaksDer.peakAcceptor = p -> p.numNonZeroPts >= 3;
      optsPeaksDer.zeroTolerance = 1e-8;
      List<PeakApprox> yPeaks = detectPeakLocations(yVals, optsPeaksDer);
      yPeaks.removeIf(p -> p.valTop == 0.0);
      yPeaks.sort((p1, p2) -> Double.compare(p2.valTop, p1.valTop));
      List<Peak> yPeaksFitted = new ArrayList<>(yPeaks.size());
      for (PeakApprox p : yPeaks) {
        Peak parabolicPeak = createParabolicPeak(brX.lo, true, xVals, yVals, p);
        yPeaksFitted.add(parabolicPeak);
      }
      if (yPeaksFitted.isEmpty()) {
        log.warn("No peaks detected while selecting seed point for noise estimation.");
      }
      yPeaksFitted.sort((p1, p2) -> Double.compare(p2.y, p1.y));

      // mu of the 'wide' noise distrubution
      double ySum = 0;
      double wSum = 0;
      for (int i = 0; i < xVals.length; i++) {
        ySum += yVals[i];
        wSum += yVals[i] * xVals[i];
      }

      d1mu = yPeaksFitted.get(0).x;
      d2mu = wSum / ySum;
      d1sd = 0.02 / 3;
      d2sd = d1sd * 2;
      d1w = 2d / 3d;
      d2w = 1d / 3d;
      return this;
    }
  }
}
