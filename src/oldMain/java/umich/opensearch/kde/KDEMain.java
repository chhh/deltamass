package umich.opensearch.kde;

import com.github.chhh.utils.LogUtils;
import com.github.chhh.utils.MathUtils;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.files.FileListing;
import com.google.common.base.Charsets;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import jsat.distributions.empirical.KernelDensityEstimator;
import jsat.distributions.empirical.kernelfunc.KernelFunction;
import jsat.linear.DenseVector;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.YIntervalRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.stat.distribution.Mixture;
import umich.opensearch.kde.api.IWeightedData;
import umich.opensearch.kde.api.PepXmlContent;
import umich.opensearch.kde.api.SearchHitDiffs;
import umich.opensearch.kde.gui.KDEGuiProps;
import umich.opensearch.kde.gui.KDEOptionsGui;
import umich.opensearch.kde.impl.WeightedDataDelimitedFile;
import umich.opensearch.kde.impl.WeightedDataPepXmlList;
import umich.opensearch.kde.jfree.JFreeChartPlot;
import umich.opensearch.kde.jsat.KDEKludge;
import umich.opensearch.kde.logging.LogHelper;
import umich.opensearch.kde.params.NamedBandwidth;
import umich.opensearch.kde.params.kernels.GaussFasterKF;
import umich.opensearch.kde.util.*;
import umich.ptm.PtmFactory;
import umich.ptm.exceptions.ModParsingException;
import umich.ptm.mod.Mod;
import umich.ptm.mod.Mods;

/**
 * \* @author Dmitry Avtonomov
 */
public class KDEMain {

  public static final Paint[] COLORS_NICE = new Paint[]{
      Color.decode("#a6cee3"),
      Color.decode("#1f78b4"),
      Color.decode("#b2df8a"),
      Color.decode("#33a02c"),
      Color.decode("#fb9a99"),
      Color.decode("#e31a1c"),
      Color.decode("#fdbf6f"),
      Color.decode("#ff7f00"),
      Color.decode("#cab2d6"),
      Color.decode("#6a3d9a"),
      Color.decode("#ffff99"),
      Color.decode("#b15928"),
  };
  public static final Paint[] COLORS_BETTER = new Paint[]{
      new Color(106, 61, 154),
      new Color(255, 127, 0),
      new Color(227, 26, 28),
      new Color(51, 160, 44),
      new Color(31, 120, 180),
      new Color(202, 178, 214),
      new Color(253, 191, 111),
      new Color(251, 154, 153),
      new Color(178, 223, 138),
      new Color(166, 206, 227),
  };
  private static final Logger log = LoggerFactory.getLogger(KDEMain.class);
  private static final boolean DEBUG = false;
  public static Paint[] COLORS_UGLY = new Paint[]{Color.red, Color.blue, Color.green, Color.orange,
      Color.magenta, Color.PINK};
  private JFreeChartPlot.ChartCloseOption chartCloseOption = JFreeChartPlot.ChartCloseOption.EXIT;

  /**
   * Original main entry point. Changed to {@link #run(umich.opensearch.kde.OpenSearchParams)} now
   * as the same code is reused by the GUI version.
   */
  public static void main(String[] args) {
    LogHelper.configureJavaUtilLogging();

    KDEMain kdeMain = new KDEMain();

    boolean isFlush = Arrays.stream(args).anyMatch("--flush"::equals);
    if (isFlush) {
      KDEGuiProps.deleteTemp();
    }

    // Need to check for gui flag separately as "-i" is required in the params.
    // Check will fail during parameter parsing if only `java -jar ... --gui" is executed.
    boolean isGui = Arrays.stream(args).anyMatch("--gui"::equals);
    boolean isInputSpecified = Arrays.stream(args)
        .anyMatch(s -> "-i".equals(s) || "--in".equals(s));

    OpenSearchParams params;
    if (isGui && !isInputSpecified) {
      params = new OpenSearchParams();
      params.setRunGui(true);
    } else {
      params = OpenSearchParams.parseParams(args, System.err);
    }

    if (params == null) {
      return;
    }
    kdeMain.run(params);
  }

  public static void runKde(OpenSearchParams params, List<PepXmlContent> pepxmls) {

  }

  /**
   * Run the KDE with plotting and peak output.
   *
   * @param params Options.
   * @param kdeMain This is really only used to set up the action of the window closing operation.
   * In full GUI mode, this allows the main window not to close, when you close KDE plot. If null,
   * closing the KDE plot will always shut down the application.
   * @param pepXmlList Can be null, in which case the info viewer won't be set up.
   */
  public static void runKde(OpenSearchParams params, KDEMain kdeMain,
      List<PepXmlContent> pepXmlList, IWeightedData diffsWeighted) {
    log.info("Running Kernel Density Estimation");

    double[] massDiffs = diffsWeighted.getData();
    double[] weights = diffsWeighted.getWeights();
    log.info(String.format("Sorting input data.. (%d points)", massDiffs.length));
    Arrays.sort(massDiffs);
    log.info("Done");

    // setting up axes
    double lo = params.getMzLo() == null ? massDiffs[0] : params.getMzLo();
    double hi = params.getMzHi() == null ? massDiffs[massDiffs.length - 1] : params.getMzHi();
    double step = params.getMzStep();
    double[] xAxis = createAxis(lo, hi, step);
    final String yAxisLblDer = "Der(2)";
    final String yAxisLblBfgs = "BFGS";

    log.info("Setting up KDE, might take a while..");
    List<KdeResult> kdeResults = new ArrayList<>();
    List<IndexBracket> brXaxis = BracketUtils.findIntegerBrackets(0.5, xAxis);
    List<NamedBandwidth> bandwidths = params.getNamedBandwidths();
    for (NamedBandwidth bandwidth : bandwidths) {

      log.info(String.format("Bandwidth: %s", bandwidth.getName()));

      double[] yAxis = new double[xAxis.length];
      double[] yAxisLowRemoved = new double[xAxis.length];
      double[] yAxisDer = new double[xAxis.length];
      double h = Double.NaN;

      KdeResult kdeResult = new KdeResult();
      kdeResult.step = step;
      kdeResult.kdeXAxis = xAxis;
      kdeResult.kdeYAxis = yAxis;

      // deprecated
      if (bandwidth.isBandwidthAutoAtFixedMz()) {
        // dynamic bandwidth estimation at a single predefined m/z
        double baseMz = params.getAutoBandwithTarget();
        double baseWnd = params.getAutoBandwidthWindow();
        double loVal = baseMz - baseWnd;
        double hiVal = baseMz + baseWnd;
        IndexBracket bracket = BracketUtils.findBracket(loVal, hiVal, massDiffs);
        if (bracket.size < 5) {
          log.info(String.format(
              "Requested dynamic bandwidth estimation at m/z [%.2f], " +
                  "but less than 5 data points were found there.", baseMz));
          return;
        }
        DenseVector baseVec = new DenseVector(massDiffs, bracket.lo, bracket.hi);
        h = KernelDensityEstimator.BandwithGuassEstimate(baseVec);
      } else if (!Double.isNaN(bandwidth.getBandwidth())) {
        // fixed bandwidth
        h = bandwidth.getBandwidth();
      }

      for (IndexBracket brX : brXaxis) {
        log.info(String.format("Processing interval [%.2f to %.2f]", brX.loVal, brX.hiVal));
        IndexBracket brD = BracketUtils.findBracket(brX.loVal, brX.hiVal, massDiffs);
        if (brD.size == 0) {
          continue;
        }
        DenseVector vecLocal = new DenseVector(massDiffs, brD.lo, brD.hi);

        // in case bandwidth needs to be estimated at each nominal mass separately
        if (bandwidth.isBandwidthAuto()) {
          double baseMz = Math.ceil(brX.loVal);
          double baseWnd = bandwidth.getWindow();
          double loVal = baseMz - baseWnd;
          double hiVal = baseMz + baseWnd;
          IndexBracket bracket = BracketUtils.findBracket(loVal, hiVal, massDiffs);
          if (bracket.size == 0) {
            continue;
            //throw new IllegalStateException("Should not happen, we already made vec from brD just fine");
          }
          DenseVector baseVec = new DenseVector(massDiffs, bracket.lo, bracket.hi);
          h = KernelDensityEstimator.BandwithGuassEstimate(baseVec);
        }

        KernelFunction kernel = params.getKernelType().kernel;
        KDEKludge kde = weights == null ? new KDEKludge(vecLocal, kernel, h)
            : new KDEKludge(vecLocal, kernel, h, weights);

        final int minPtsKdeTest = params.getKdeMinPts();
        DenseVector testVec = new DenseVector(new double[]{0d, 0.49, 0.5, 0.51});
        int vecLocalLen = vecLocal.length();
        KDEKludge kdeTestLow = new KDEKludge(testVec, kernel, h,
            new double[]{minPtsKdeTest, vecLocalLen / 4d, vecLocalLen / 2d, vecLocalLen / 4d});
        final double kdeMinAbundance = kdeTestLow.pdf(0);

        // calculating KDE profile
        double pdfArea = 0;
        for (int i = brX.lo; i < brX.hi; i++) {
          double x = xAxis[i];
          double pdf = kde.pdf(x);
          yAxis[i] = pdf;
          yAxisLowRemoved[i] = yAxis[i] < kdeMinAbundance ? 0 : yAxis[i];
          yAxisDer[i] = yAxis[i] < kdeMinAbundance ? 0 : kde.pdfPrime2(x);
          double area = pdf * step;
          pdfArea += area;
        }

        // normalize the KDE plot scale to the number of PSMs
        double density = vecLocal.length() / pdfArea;
        for (int i = brX.lo; i < brX.hi; i++) {
          yAxis[i] *= density; // KDE is now scaled to the number of points in the bracket
          yAxisLowRemoved[i] *= density;
        }

        if (params.isDetectPeaks()) {
          PeakUtils.NoiseResult noiseEstimate = PeakUtils
              .estimateNoiseDistribution(params, brX, xAxis, yAxisLowRemoved, vecLocal);

          PeakUtils.BracketResult bracketResult =
              PeakUtils.findPeaksGMM4(params, brX, xAxis, yAxisLowRemoved, yAxisDer, vecLocal);
          kdeResult.bracketResults.put(brX, bracketResult);
        }

        log.info("Done");
      }

      // finalize peak detection, including filtering remaining peaks by intensity / PSM support
      prepareResults(kdeResult, params);

      kdeResult.namedBandwidth = bandwidth;
      kdeResult.kdeXAxis = xAxis;
      kdeResult.kdeYAxis = yAxis;
      kdeResult.yAxes.put(yAxisLblDer, yAxisDer);

      kdeResults.add(kdeResult);
    }

    // write out detected peaks
    if (params.isDetectPeaks() && params.outFilePath != null) {
      for (KdeResult kdeResult : kdeResults) {
        writePeaks(params, kdeResult);
      }
    }

    if (params.isPlotKde()) {

      JFreeChartPlot plot = new JFreeChartPlot("KDE Open Search");
      DefaultXYDataset datasetKde = new DefaultXYDataset();
      DefaultXYDataset datasetGmms = new DefaultXYDataset();
      String datasetIdKde = "KDE";
      String datasetIdGmm = "GMM";
      YIntervalSeriesCollection datasetPeaks = new YIntervalSeriesCollection();
      DefaultXYDataset datasetDer = new DefaultXYDataset();

      for (KdeResult kdeResult : kdeResults) {

        // plot KDE itself
        String seriesIdBase = kdeResult.namedBandwidth.getName();
        double[] kdeXAxis = kdeResult.kdeXAxis;
        double[] kdeYAxis = kdeResult.kdeYAxis;
        ArrayFilter zeroFilter = new ArrayFilter(kdeXAxis, kdeYAxis);
        zeroFilter = zeroFilter.filterZeroesLeaveFlanking();
        datasetKde.addSeries("KDE:" + seriesIdBase,
            new double[][]{zeroFilter.getMass(), zeroFilter.getInts()});

        if (params.isPlotDer2() && kdeResult.yAxes.get(yAxisLblDer) != null) {
          double[] yAxis = kdeResult.yAxes.get(yAxisLblDer);
          ArrayFilter zf = new ArrayFilter(kdeXAxis, yAxis);
          zf.filterZeroesLeaveFlanking();
          String id = "KDE:" + seriesIdBase + "(" + yAxisLblDer + ")";
          datasetDer.addSeries(id, new double[][]{zf.getMass(), zf.getInts()});
        }

        // plot GMMs
        if (params.plotGmm && !kdeResult.bracketResults.isEmpty()) {
          int maxComps = -1;
          for (PeakUtils.BracketResult r : kdeResult.bracketResults.values()) {
            maxComps = Math.max(maxComps, r.peaksOutput.size());

            // sort the output peaks by intensity in descending order
            r.peaksOutput
                .sort((p1, p2) -> Double.compare(p2.gmmComponentArea, p1.gmmComponentArea));
          }

          if (maxComps > 0) {
            double[] gmmXAxis = kdeResult.kdeXAxis;

            for (int i = 0; i < maxComps; i++) {
              String gmmSeriesId = String.format("GMM(%d):%s", i, seriesIdBase);
              double[] gmmYAxis = new double[gmmXAxis.length];

              for (Map.Entry<IndexBracket, PeakUtils.BracketResult> e : kdeResult.bracketResults
                  .entrySet()) {
                PeakUtils.BracketResult r = e.getValue();
                if (r.peaksOutput.isEmpty()) {
                  continue;
                }

                if (i >= r.peaksOutput.size()) {
                  continue;
                }
                final PeakOutput p = r.peaksOutput.get(i);

                final double sd3 = p.sd * 3;
                final double mu = p.mean;
                double factor = p.kdeTotalArea / p.gmmCombinedArea;
                for (int j = r.bracket.lo; j < r.bracket.hi; j++) {
                  double x = gmmXAxis[j];
                  if (Math.abs(mu - x) < sd3) {
                    gmmYAxis[j] += p.distribution.p(x) * p.priori * factor;
                  }
                }

              }

              ArrayFilter zf = new ArrayFilter(gmmXAxis, gmmYAxis);
              zf = zf.filterZeroesLeaveFlanking();
              datasetGmms.addSeries(gmmSeriesId, new double[][]{zf.getMass(), zf.getInts()});


            }
          }
        }

        // plot peaks as vertical lines
        final List<PeakOutput> peakOutputs = gatherPeaks(kdeResult, params);
        if (!peakOutputs.isEmpty()) {
          YIntervalSeries peakSeries = new YIntervalSeries(
              "Peak locations (" + kdeResult.namedBandwidth.getName() + ")");
          for (PeakOutput p : peakOutputs) {
            peakSeries.add(p.der2Loc, 0, 0, p.gmmComponentScaledHeight);
          }
          datasetPeaks.addSeries(peakSeries);
        }
      }

      // adding dataset to the plot
      if (datasetKde.getSeriesCount() > 0) {
        XYLineAndShapeRenderer render = new XYLineAndShapeRenderer(true, false) {
          @Override
          public Paint getSeriesPaint(int series) {
            return COLORS_BETTER[series % COLORS_BETTER.length];
          }
        };

        plot.addDataset(datasetIdKde, datasetKde, render, false);
      }

      // configuring plotting
      if (datasetGmms.getSeriesCount() > 0) {
        final BasicStroke dashedStroke = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            1.0f, new float[]{3.0f, 3.0f}, 0.0f);
        XYLineAndShapeRenderer render = new XYLineAndShapeRenderer(true, false) {
          @Override
          public Stroke getItemStroke(int row, int column) {
            return dashedStroke;
          }

          @Override
          public Paint getSeriesPaint(int series) {
            return COLORS_BETTER[series % COLORS_BETTER.length];
          }
        };
        render.setDrawSeriesLineAsPath(true);
        plot.addDataset(datasetIdGmm, datasetGmms, render, false);
      }
      if (datasetPeaks.getSeriesCount() > 0) {
        YIntervalRenderer renderer = new YIntervalRenderer() {
          @Override
          public Paint getSeriesPaint(int series) {
            return COLORS_BETTER[series % COLORS_BETTER.length];
          }
        };
        plot.addDataset("Local Maxima", datasetPeaks, renderer, false);
      }
      if (datasetDer.getSeriesCount() > 0) {
        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
        r.setBaseStroke(new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            1.0f, new float[]{3.0f, 3.0f}, 0.0f));
        r.setDrawSeriesLineAsPath(true);
        plot.addDataset("Derivatives", datasetDer, r, true);
      }

      if (kdeMain != null) {
        plot.display(kdeMain.chartCloseOption.windowConstant, params.getInFilePath().toString());
      } else {
        plot.display(WindowConstants.EXIT_ON_CLOSE, params.getInFilePath().toString());
      }

      if (pepXmlList != null) {
        try {
          Mods mods = PtmFactory
              .getMods(EnumSet.of(PtmFactory.SOURCE.GEN, PtmFactory.SOURCE.UNIMOD));
          plot.setMods(mods);
        } catch (ModParsingException e) {
          log.error("Could not parse modifications from modification sources", e);
          System.exit(1);
        }
        plot.addMouseListener(pepXmlList);
      }

    }

  }

  private static void writePeaks(OpenSearchParams params, KdeResult kdeResult) {
    //CSVFormat csvFormat = CSVFormat.newFormat(',').withHeader("dm", "sd", "weight")
    //        .withQuoteMode(QuoteMode.NON_NUMERIC).withRecordSeparator("\r\n").withSkipHeaderRecord(false);

    final List<PeakOutput> peakOutputs = gatherPeaks(kdeResult, params);
    // sort the output peaks by delta mass in ascending order
    peakOutputs.sort(Comparator.comparingDouble(p2 -> p2.der2Loc));

    // PTM annotations
    Mods mods;
    try {
      mods = PtmFactory.getMods(EnumSet.of(PtmFactory.SOURCE.GEN, PtmFactory.SOURCE.UNIMOD));
    } catch (ModParsingException e) {
      throw new IllegalStateException("Could not create a list of annotation PTMs");
    }

    final double sdDistance = 2.0; // annotations max 2 SD away from peaks in KDE
    final double maxAbsDistance = 0.001;
    final DecimalFormat df4 = new DecimalFormat("0.0000");
    final DecimalFormat df1 = new DecimalFormat("0.0");
    final String valSep = "::";
    final String entrySep = "; ";

    final List<OutputEntry> list = new ArrayList<>();
    for (PeakUtils.BracketResult r : kdeResult.bracketResults.values()) {
      for (PeakOutput p : r.peaksOutput) {
        String annotation = mods
            .findByMass(p.der2Loc - sdDistance * p.sd, p.der2Loc + sdDistance * p.sd).stream()
            .map(mod -> {
              final Double massMono = mod.getComposition().getMassMono();
              if (massMono == null) {
                log.error(
                    "Monoisotopic mass of elemental composition was null while mapping possible mods for a peak");
              }
              final double dm = massMono - p.der2Loc;
              final double likelihood = p.distribution.likelihood(new double[]{massMono});
              return new ModEntry(massMono, dm, likelihood, mod);
            })
            .sorted((o1, o2) -> Double.compare(o2.likelihood, o1.likelihood))
            .map(me -> {
              StringBuilder desc = new StringBuilder();
              desc.append(me.mod.getRef()).append(valSep)
                  .append("M=").append(df4.format(me.m)).append("Da").append(valSep)
                  .append("dM=").append(df4.format(me.dm)).append("Da").append(valSep)
                  .append("q=").append(df4.format(me.likelihood)).append(valSep)
                  .append(me.mod.getDescShort());
              return desc.toString();
            })
            .collect(Collectors.joining(entrySep, "\"", "\""));

        double dm = p.der2Loc;
        double sd = p.sd;
        double support = p.gmmSupport;
        double intensity = p.gmmComponentScaledHeight;
        double quality = p.der2Value;
        double score = quality * intensity;
        list.add(new OutputEntry(dm, sd, support, intensity, quality, score, annotation));
      }
    }
    log.info("Created output list of {} peaks", list.size());
    // sort by score
    list.sort((o1, o2) -> Double.compare(o2.score, o1.score));
    log.info("Sorted by descending score");

    // write the list to the file
    Path outputPath = kdeResult.namedBandwidth.createOutputPath(params.outFilePath);
    log.info("Writing peaks to file: {}", outputPath);
    try (OutputStreamWriter osw = new OutputStreamWriter(
        new FileOutputStream(outputPath.toFile(), false), Charsets.UTF_8)) {
      osw.write("dm, fwhm, support, intensity, quality, score, annotations\n");
      for (OutputEntry oe : list) {
        String format = String.format("%.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %s\n",
            oe.dm, oe.fwhm, oe.support, oe.intensity, oe.quality, oe.score, oe.annotations);
        osw.write(format);
      }
      osw.flush();
    } catch (FileNotFoundException e) {
      log.error("Error while writing output, file not found");
    } catch (IOException e) {
      log.error("Error while writing output", e);
    }
    log.info("Done writing peaks");
  }

  private static void prepareResults(KdeResult kdeResult, OpenSearchParams params) {
    if (kdeResult.bracketResults.isEmpty()) {
      return;
    }

    double[] kdeXAxis = kdeResult.kdeXAxis;
    double[] kdeYAxis = kdeResult.kdeYAxis;
    double step = kdeResult.step;

    // GMMs
    for (Map.Entry<IndexBracket, PeakUtils.BracketResult> entry : kdeResult.bracketResults
        .entrySet()) {
      PeakUtils.BracketResult r = entry.getValue();
      if (r.gmm == null || r.gmm.getComponents().isEmpty()) {
        continue;
      }

      // area under the KDE curve
      // at this point is should be equal to the number of PSM entries in the bracket
      double kdeArea = 0;
      for (int j = r.bracket.lo; j < r.bracket.hi; j++) {
        kdeArea += step * kdeYAxis[j];
      }

      // area of all GMM components
      double gmmCombinedArea = 0;
      for (Mixture.Component mc : r.gmm.getComponents()) {
        for (int j = r.bracket.lo; j < r.bracket.hi; j++) {
          gmmCombinedArea += step * mc.distribution.p(kdeXAxis[j]) * mc.priori;
        }
      }

      // find the highest GMM component (before mapping peaks to GMM components)
      double topGmmCompIntensity = 0;
      for (Mixture.Component mc : r.gmm.getComponents()) {
        double gmmPeakHeight = mc.distribution.p(mc.distribution.mean()) * mc.priori;
        if (gmmPeakHeight > topGmmCompIntensity) {
          topGmmCompIntensity = gmmPeakHeight;
        }
      }

      // map peaks from 2nd derivative to GMM components
      final TreeMap<Double, Mixture.Component> gmmMap = new TreeMap<>();
      for (Mixture.Component mc : r.gmm.getComponents()) {
        gmmMap.put(mc.distribution.mean(), mc);
      }
      TreeMap<Peak, Mixture.Component> peakMapping = new TreeMap<>();
      final HashMap<Mixture.Component, List<Peak>> gmmToPeakList = new HashMap<>();
      //final TreeMap<ComparableHolder<Double, Mixture.Component>, List<Peak>> gmmToPeakList2 = new TreeMap<>();
      for (Peak p : r.peaks) {
        final Mixture.Component mc = MapUtils.findClosest(gmmMap, p.x);
        if (mc == null) {
          continue;
        }
        double distance = Math.abs(mc.distribution.mean() - p.x);
        // if a peak is more than 1 SD away from the mean of a GMM component - disregard it
        if (distance > mc.distribution.sd()) {
          continue;
        }
        peakMapping.put(p, mc);
        List<Peak> list = gmmToPeakList.computeIfAbsent(mc, k -> new ArrayList<>());
        list.add(p);

        //List<Peak> list2 = gmmToPeakList2.computeIfAbsent(new ComparableHolder<>(mc.distribution.mean(), mc), k -> new ArrayList<>());
        //list2.add(p);
      }

      // iterate over mixture components mapped to peaks
      for (Map.Entry<Mixture.Component, List<Peak>> e : gmmToPeakList.entrySet()) {
        final Mixture.Component mc = e.getKey();

        // check if GMM component's height is enough relative to the top peak at this nominal delta mass
        double gmmPeak = mc.distribution.p(mc.distribution.mean()) * mc.priori;
        if (gmmPeak < topGmmCompIntensity * params.cutoff) {
          //LogHelper.logMsg(String.format("Removing peak, not high enough (%.2f) relative to top (%.2f)", gmmPeak, gmmTop), out, log, true);
          continue;
        }

        // volume of the current GMM component (should be roughly 1.0)
        double cArea = 0;
        for (int j = r.bracket.lo; j < r.bracket.hi; j++) {
          cArea += step * mc.distribution.p(kdeXAxis[j]) * mc.priori;
        }

        // check if the GMM component is supported by enough PSMs
        final double factor = kdeArea / gmmCombinedArea;
        double gmmSupport = cArea * factor;
        if (gmmSupport < params.gmmMinSupport) {
          //LogHelper.logMsg(String.format("Removing peak, not enough support (%.2f), min support needed(%d)", gmmSupport, params.gmmMinSupport), out, log, true);
          continue;
        }

        final double gmmPeakHeight = mc.distribution.p(mc.distribution.mean()) * mc.priori * factor;

        // take the highest peak
        final List<Peak> peaks = e.getValue();
        if (peaks.isEmpty()) // unlikely
        {
          continue;
        }
        if (peaks.size() > 1) {
          peaks.sort((p1, p2) -> Double.compare(p2.peak.amplitudeLo(), p1.peak.amplitudeLo()));
        }
        Peak p = peaks.get(0);

        // all tests passed, this peak and GMM component can be reported
        final PeakOutput.Builder b = new PeakOutput.Builder();
        b.setMean(mc.distribution.mean())
            .setSd(mc.distribution.sd())
            .setPriori(mc.priori)
            .setDistribution(mc.distribution)
            .setKdeTotalArea(kdeArea)
            .setKdeTotalEntries(r.numEntries)
            .setGmmComponentArea(cArea)
            .setGmmCombinedArea(gmmCombinedArea)
            .setGmmComponentScaledHeight(gmmPeakHeight)
            .setGmmSupport(gmmSupport)
            .setDer2Loc(p.x)
            .setDer2Amplitude(p.peak.amplitudeLo())
            .setDer2Value(p.peak.valTop);

        // add the peak to the final list
        final PeakOutput outPeak = b.createPeakOutput();
        if (outPeak.der2Value
            < 0) { // der2 values are inverted, hence the check for < 0 instead of > 0
          continue;
        }
        r.peaksOutput.add(outPeak);
      }
      r.peaksOutput.sort(Comparator.comparingDouble(o -> o.der2Loc));
    }
  }

  private static List<PeakOutput> gatherPeaks(KdeResult kdeResult, OpenSearchParams params) {
    ArrayList<PeakOutput> res = new ArrayList<>();

    for (PeakUtils.BracketResult br : kdeResult.bracketResults.values()) {
      res.addAll(br.peaksOutput);
    }

    return res;
  }

  private static void plotFirstDerivative(Appendable out, double lo, double hi, double step,
      double[] xAxis, KDEKludge kde, String datasetId, JFreeChartPlot plot) {
    String msg;
    double h = 0.001;
    String seriesIdDer = "der2@0.001h";

    msg = String.format("Calcing KDE for dataset: %s\n", seriesIdDer);
    log.info(msg);
    LogUtils.println(out, msg);

    kde.setBandwith(h);
    kde.setKernelFunction(GaussFasterKF.getInstance());
    double cur = lo, val;
    int curIdx = 0;
    double integralPdf = 0.0d;
    double[] yAxis = new double[xAxis.length];
    while (cur <= hi) {
      val = kde.pdfPrime2(cur);
      yAxis[curIdx] = val;
      integralPdf += val * step;
      curIdx++;
      cur += step;
    }
    msg = String.format("\tTotal area under KDEder : %.4f\n", integralPdf);
    log.info(msg);
    LogUtils.println(out, msg);
    ArrayFilter zeroFilterDer = new ArrayFilter(xAxis, yAxis);
    zeroFilterDer = zeroFilterDer.filterZeroesLeaveFlanking();
    DefaultXYDataset dataset = new DefaultXYDataset();
    dataset
        .addSeries(seriesIdDer, new double[][]{zeroFilterDer.getMass(), zeroFilterDer.getInts()});
    plot.addDataset("Derivative", dataset, new XYLineAndShapeRenderer(true, false), true);
  }

  private static double[] createAxis(double lo, double hi, double step) {
    int numPts = (int) ((hi - lo) / step) + 1;
    if (numPts < 2) {
      throw new IllegalStateException(
          "For some reason x axis only contained 2 points, probably incorrect parameters");
    }
    double[] xAxis = new double[numPts];
    xAxis[0] = lo;
    for (int i = 1; i < xAxis.length; i++) {
      xAxis[i] = xAxis[i - 1] + step;
    }
    return xAxis;
  }

  private static void deleteCache(OpenSearchParams params) {
    log.info("Delete cache option was specified, will delete cache now");
    PepXmlContent.deleteCache(params);
    return;
  }

  public static List<Path> findMatchingPaths(OpenSearchParams params) {
    List<Path> matchingPaths = new ArrayList<>();
    List<Path> inFilePath = params.getInFilePath();
    for (Path path : inFilePath) {
      if (!Files.isDirectory(path)) {
        matchingPaths.add(
            path); // if a particular file was specified on the cmd line, just add it even if it doesn't match
        continue;
      }
      FileListing fileListing = new FileListing(path, params.getInFileRegex());
      fileListing.setFollowLinks(params.isFollowSymlinks());
      fileListing.setIncludeDirectories(false);
      fileListing.setIncludeFiles(true);
      List<Path> files = fileListing.findFiles();
      matchingPaths.addAll(files);

    }
    return matchingPaths;
  }

  public JFreeChartPlot.ChartCloseOption getChartCloseOption() {
    return chartCloseOption;
  }

  public void setChartCloseOption(JFreeChartPlot.ChartCloseOption chartCloseOption) {
    this.chartCloseOption = chartCloseOption;
  }

  /**
   * New main entry point. Consider using {@link OpenSearchParams#parseParams(String[], Appendable)
   * } to actually parse the parameters if you have them in command line form.
   */
  public void run(OpenSearchParams params) {
    log.debug("Running with parameters:\n{}", params.toString());

    if (params.isRunGui()) {
      if (!SwingUtils.isGraphicalEnvironmentAvailable()) {
        log.error("Graphcal environment is needed to run the GUI.");
        return;
      }
      KDEOptionsGui.initAndRun(params);
      return;
    } else {
      final SplashScreen splash = SplashScreen.getSplashScreen();
      if (splash != null) {
        splash.close();
      }
    }

    if (Thread.interrupted()) {
      return;
    }

    if (params.isDeleteCache()) {
      deleteCache(params);
      return;
    }

    // check bandwidths
    List<NamedBandwidth> bandwidths = params.getNamedBandwidths();
    if (bandwidths == null || bandwidths.isEmpty()) {
      log.error("Bandwidths list can't be empty.");
      return;
    }

    // get the data in
    IWeightedData diffsWeighted;
    List<PepXmlContent> pepXmlList = null;
    List<Path> matchingPaths = findMatchingPaths(params);
    if (!params.isBinaryInput()) {
      // get the data in: for regular pep xml input
      String foundFilesStr = String
          .format("Found %d files matching input (single files and '%s' pattern) in '%s'",
              matchingPaths.size(), params.getInFileRegex(),
              StringUtils.join(params.getInFilePath(), ", "));
      log.info(foundFilesStr);

      if (matchingPaths.size() == 0) {
        log.warn("No files found - nothing to do, exiting.");
        return;
      }
      pepXmlList = PepXmlContent.getPepXmlContents(params, matchingPaths);
      log.info(String.format("Loaded %d search results files", pepXmlList.size()));
      int searchHitCount = 0;
      for (PepXmlContent pepXmlContent : pepXmlList) {
        for (SearchHitDiffs searchHitDiffs : pepXmlContent.getHitsList()) {
          searchHitCount += searchHitDiffs.getHits().size();
        }
      }
      log.info(String.format("It contained %d hits before filtering", searchHitCount));

      diffsWeighted = WeightedDataPepXmlList.create(pepXmlList, params);
      int totalEntries = 0;
      for (PepXmlContent pepXmlContent : pepXmlList) {
        List<SearchHitDiffs> hitsList = pepXmlContent.getHitsList();
        for (SearchHitDiffs searchHitDiffs : hitsList) {
          log.info(String.format("Sorting entries of '%s'", searchHitDiffs.getName()));
          searchHitDiffs.sortByDiffMass();
          totalEntries += searchHitDiffs.getHits().size();
        }
      }

      if (totalEntries < 50) {
        log.warn(
            "There were less than 50 entries in the parsed/filtered data set. Won't run KDE on such small data.");
        return;
      }

    } else {
      // get the data in: for delimited text file with numbers
      ArrayList<double[][]> inputData = new ArrayList<>(matchingPaths.size());
      for (Path inFilePath : matchingPaths) {
        double[][] data = BinReader.readData(inFilePath, false, params);
        inputData.add(data);
      }
      diffsWeighted = WeightedDataDelimitedFile.create(inputData);
    }
    if (Thread.interrupted()) {
      return;
    }

    // do the plotting and peak finding
    if (params.isPlotKde()) {
      if (!SwingUtils
          .isGraphicalEnvironmentAvailable()) { // this can only happen in console mode, so no worries
        log.error("Graphical environment is needed to run the GUI, won't plot KDE");
        return;
      }
    }
    if (params.isPlotKde() || params.isDetectPeaks()) {
      // there is no reason to do KDE if we're not plotting or detecting peaks
      runKde(params, this, pepXmlList, diffsWeighted);
    }


  }

  private static class OutputEntry {

    final double dm;
    final double sd;
    final double fwhm;
    final double support;
    final double intensity;
    final double quality;
    final double score;
    final String annotations;

    public OutputEntry(double dm, double sd, double support, double intensity, double quality,
        double score, String annotations) {
      this.dm = dm;
      this.sd = sd;
      this.fwhm = MathUtils.fwhmFromSigma(sd);
      this.support = support;
      this.intensity = intensity;
      this.quality = quality;
      this.score = score;
      this.annotations = annotations;
    }
  }

  private static class ModEntry {

    final double m;
    final double dm;
    final double likelihood;
    final Mod mod;

    public ModEntry(double m, double dm, double likelihood, Mod mod) {
      this.m = m;
      this.dm = dm;
      this.likelihood = likelihood;
      this.mod = mod;
    }
  }

  private static class KdeResult {

    NamedBandwidth namedBandwidth = null;
    double[] kdeXAxis = null;
    double step;
    double[] kdeYAxis = null;
    HashMap<String, double[]> yAxes = new HashMap<>();
    TreeMap<IndexBracket, PeakUtils.BracketResult> bracketResults = new TreeMap<>();

    public KdeResult() {
    }
  }
}
