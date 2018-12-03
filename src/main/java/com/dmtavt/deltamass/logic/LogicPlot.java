package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.DeltaMassInfo;
import com.dmtavt.deltamass.data.PepSearchFile;
import com.dmtavt.deltamass.logic.LogicKde.ProcessingSegment;
import com.dmtavt.deltamass.logic.LogicPeaks.Detected;
import com.dmtavt.deltamass.logic.LogicPeaks.GmmComponent;
import com.dmtavt.deltamass.ui.PlotFactory;
import com.dmtavt.deltamass.ui.PlotFactory.CloseOption;
import com.dmtavt.deltamass.utils.GridUtils;
import com.dmtavt.deltamass.utils.PeakUtils.PeakDetectionConfig;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.color.ColorHelper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jsat.distributions.Normal;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.YIntervalRenderer;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.YIntervalDataItem;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicPlot {

  private static final Logger log = LoggerFactory.getLogger(LogicPlot.class);
  final CommandPlot cmd;

  public static final Paint[] COLORS_BETTER = new Paint[] {
      new Color(106,61,154),
      new Color(255,127,0),
      new Color(227,26,28),
      new Color(51,160,44),
      new Color(31,120,180),
      new Color(202,178,214),
      new Color(253,191,111),
      new Color(251,154,153),
      new Color(178,223,138),
      new Color(166,206,227),
  };

  public static final Paint[] COLORS_GMMS = new Paint[] {
//      new Color(0,60,0),
      new Color(42, 49, 179),
      new Color(210, 0, 12),
      new Color(25, 152, 34),
  };

  public LogicPlot(CommandPlot cmd) {
    this.cmd = cmd;
  }

  public void run() {
    if (!SwingUtils.isGraphicalEnvironmentAvailable()) {
      log.error("No graphical environment, can't plot.");
      return;
    }

    LogicInputFiles inputFiles = new LogicInputFiles(cmd.optsInputFiles);

    final List<PepSearchFile> searchFiles;
    try {
      searchFiles = inputFiles.run();
    } catch (Exception e) {
      log.error("Error collecting input data, stopping");
      return;
    }

    LogicKde logicKde = new LogicKde(cmd.optsKde, searchFiles);
    List<ProcessingSegment> kde = logicKde.run();
    plot(kde);
  }

  private void plot(List<ProcessingSegment> segments) {
    log.debug("Start plot(segments)");
    double xLo = Double.POSITIVE_INFINITY;
    double xHi = Double.NEGATIVE_INFINITY;
    for (ProcessingSegment seg : segments) {
      if (seg.x[0] < xLo)
        xLo = seg.x[0];
      if (seg.x[seg.x.length - 1] > xHi)
        xHi = seg.x[seg.x.length - 1];
    }
    final double plottingStep = cmd.optsPlot.step;
    double[] grid = GridUtils.grid(xLo, xHi, plottingStep);

    // interpolate values from KDE results onto the plotting grid
    double[] valsKde = new double[grid.length];
    double[] valsDer2 = new double[grid.length];
    LinearInterpolator interpolator = new LinearInterpolator();
//    SplineInterpolator interpolator = new SplineInterpolator();

    for (ProcessingSegment seg : segments) {
      // map data range of segment to the corresponding span in grid
      final double segXLo = seg.x[0];
      final double segXHi = seg.x[seg.x.length - 1];
      int iXLo = Arrays.binarySearch(grid, segXLo);
      iXLo = iXLo >= 0 ? iXLo : ~iXLo;
      int iXHi = Arrays.binarySearch(grid, segXHi);
      iXHi = iXHi >= 0 ? iXHi : ~iXHi;

      PolynomialSplineFunction iKde = interpolator.interpolate(seg.x, seg.kde);
      PolynomialSplineFunction iDer = interpolator.interpolate(seg.x, seg.der2);
;
      for (int i = iXLo, cap = iXHi; i < cap; i++) {
        final double gridVal = grid[i];
        valsKde[i] = iKde.value(gridVal) * seg.data.spmInfos.size();
        valsDer2[i] = iDer.value(gridVal);
      }
    }

    // Dataset KDE
    log.debug("Dataset KDE");
    final DefaultXYDataset datasetKde = new DefaultXYDataset();
    final String seriesKdeKey = "KDE";
    datasetKde.addSeries(seriesKdeKey, new double[][]{grid, valsKde});

    // Dataset histogram
    List<SimpleHistogramDataset> datasetHistograms = new ArrayList<>();
    final boolean doHistogram = false;
    if (doHistogram) {
      log.debug("Dataset Histogram");
      //List<Double> histDx = Arrays.asList(0.01, 0.005, 0.001);
//      List<Double> histDx = Arrays.asList(0.005, 0.001, 0.0002);
      List<Double> histDx = Arrays.asList(0.002, 0.001, 0.0005);
      histDx.sort((o1, o2) -> Double.compare(o2,o1));

      for (Double dx : histDx) {
        double histLo = Double.POSITIVE_INFINITY;
        double histHi = Double.NEGATIVE_INFINITY;
        for (ProcessingSegment seg : segments) {
          histLo = Math.min(histLo, seg.data.xLoBracket);
          histHi = Math.max(histHi, seg.data.xHiBracket);
        }
        final double offset = 0;
        double ptrLo = histLo + offset;
        final String key = String.format("Histogram-dx=%.4f", dx);
        final SimpleHistogramDataset hist = new SimpleHistogramDataset(key);
        // create bins
        while (ptrLo < histHi) {
          final double loBound = ptrLo;
          final double hiBound = ptrLo + dx;
          SimpleHistogramBin bin = new SimpleHistogramBin(loBound, hiBound, true, false);
          ptrLo = hiBound;
          // fill bin
          long pointsInBin = segments.stream().flatMap(seg -> seg.data.spmInfos.stream())
              .filter(si -> si.spm.mDiffCorrected >= loBound && si.spm.mDiffCorrected < hiBound)
              .count();
          bin.setItemCount((int)pointsInBin);
          log.trace("Assigned {} points to bin [{}, {}]", pointsInBin, loBound, hiBound);
          hist.addBin(bin);
        }
        int totalDataPoints = segments.stream().mapToInt(seg -> seg.data.spmInfos.size()).sum();
        // old bin filling
//        segments.stream().flatMap(seg -> seg.data.spmInfos.stream()).map(si -> si.spm.mDiffCorrected)
//            .forEach(mDiffCorr -> hist.addObservation(mDiffCorr, false));


        log.debug("Create dataset histogram key: {}, total num data points: {}, step: {}, offset: {}", key, totalDataPoints, dx, offset);
        datasetHistograms.add(hist);
      }
    }

    // detect peaks
    List<Detected> detecteds = new ArrayList<>();
    if (!cmd.optsPlot.noPeaks) {
      final PeakDetectionConfig conf = new PeakDetectionConfig(cmd.optsPeaks.minPeakPct,
          cmd.optsPeaks.minPsmsPerGmm);
      log.debug("Running peak detection");
      for (ProcessingSegment segment : segments) {
        detecteds.add(LogicPeaks.detectPeaks(segment, conf));
      }
    }

    // if output file given, save peaks
    final Path outPath = cmd.optsPeaks.out;
    if (outPath != null) {
      try {
        // we asked for user permission to overwrite the file before running kde
        final Path outputFilePath = LogicPeaks.getOutputFilePath(outPath);
        LogicPeaks.writePeaks(outputFilePath, detecteds, cmd.optsPeaks.digitsMz, cmd.optsPeaks.digitsAb);
      } catch (IOException e) {
        log.error("Error writing output file: " + outPath.toString(), e);
      }
    } else {
      // use for debugging only
      //LogicPeaks.writePeaks(System.err, detecteds, cmd.optsPeaks.digitsMz, cmd.optsPeaks.digitsAb);
    }

    // DatasetPeaks
    log.debug("Dataset Peaks");
    final YIntervalSeriesCollection datasetPeaks = new YIntervalSeriesCollection();
    if (!detecteds.isEmpty()) {
      final String seriesPeakLocsKey = "Peak locations";
      final YIntervalSeries peakSeries = new YIntervalSeries(seriesPeakLocsKey);
      detecteds.stream().flatMap(dp -> dp.gmms.stream().map(gmm -> gmm.peakOrigin)).forEachOrdered(
          peak -> peakSeries.add(new YIntervalDataItem(peak.location, peak.intensity, 0, peak.intensity), false));
      if (peakSeries.getItemCount() > 0) {
        datasetPeaks.addSeries(peakSeries);
      }
    }

    // Dataset GMMs
    final boolean doPlotGmms = false;
    final String datasetGmmsKey = "dataset-GMMs";
    DefaultXYDataset datasetGmms = null;
    if (doPlotGmms) {
      log.debug("Dataset GMMs");
      int maxGmmCompCount = detecteds.stream().mapToInt(d -> d.gmms.size()).max().orElse(0);
      for (Detected detected : detecteds) {
//      detected.gmms.sort((gmm1, gmm2) -> Double.compare(gmm2.weight, gmm1.weight));
        detected.gmms.sort((gmm1, gmm2) -> Double.compare(gmm1.mu, gmm2.mu));
      }
      for (int i = 0; i < maxGmmCompCount; i++) {
        final String seriesGmmKey = String.format("GMM-%02d", i + 1);
        final double[] gmmSeriesVals = new double[grid.length];
        int pointsAdded = 0;

        for (Detected detected : detecteds) {
          if (i > detected.gmms.size() - 1) {
            continue; // this segment does not have enough GMM components
          }
          GmmComponent gmc = detected.gmms.get(i);
          final double drawDistance = gmc.sigma * 2.0;
          double boundLo = gmc.mu - drawDistance;
          double boundHi = gmc.mu + drawDistance;
          int gridIdxLo = Arrays.binarySearch(grid, boundLo);
          if (gridIdxLo < 0) {
            gridIdxLo = ~gridIdxLo;
          }
          int gridIdxHi = Arrays.binarySearch(grid, boundHi);
          if (gridIdxHi < 0) {
            gridIdxHi = ~gridIdxHi;
          }
          for (int j = gridIdxLo; j < gridIdxHi; j++) {
            final double pdf = Normal.pdf(grid[j], gmc.mu, gmc.sigma); // TODO: consider using my MathUtils
            final double val = pdf * gmc.weight * gmc.psmSupportApprox;
            gmmSeriesVals[j] = val;
            pointsAdded++;
          }
        }

        datasetGmms = new DefaultXYDataset();
        if (pointsAdded > 0) {
          datasetGmms.addSeries(seriesGmmKey, new double[][]{grid, gmmSeriesVals});
        }
      }
    }

    PlotFactory pf = new PlotFactory();
    pf.setData(segments);

    final String datasetKdeKey = "dataset-KDE";
    pf.addDataset(datasetKdeKey, datasetKde, null);

    final String datasetPeaksKey = "dataset-Peaks";
    final BasicStroke datasetPeaksStroke = new BasicStroke(
        2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
        1.0f, new float[]{3.0f, 3.0f}, 0.0f);
    YIntervalRenderer datasetPeaksRenderer = new YIntervalRenderer() {
      @Override
      public Paint getSeriesPaint(int series) {
        return COLORS_BETTER[series % COLORS_BETTER.length];
      }

      @Override
      public Stroke getSeriesStroke(int series) {
        return datasetPeaksStroke;
      }
    };
    String yAxisPeakQualKey = "peak-quality";
    ValueAxis yAxisPeakQual = new NumberAxis("Peak quality");
    pf.addSecondaryYAxis(yAxisPeakQualKey, yAxisPeakQual);
    pf.addDataset(datasetPeaksKey, datasetPeaks, datasetPeaksRenderer, yAxisPeakQualKey);

    if (doHistogram) {
      // histogram goes to the secondary axis
      log.debug("Adding histogram datasets");
      final String datasetHistKeyBase = "dataset-Histogram";
      final List<Color> palette = ColorHelper
          .getDistinctColors(datasetHistograms.size(), 0.5f, 0.75f);
      final List<Color> histColors = new ArrayList<>();
      for (Color c : palette) {
        final int histAlpha = 100;
        Color cTransparent = new Color(c.getRed(), c.getGreen(), c.getBlue(), histAlpha);
        histColors.add(cTransparent);
      }

      String yAxisHistKey = "histogram";
      ValueAxis yAxisHist = new NumberAxis("Count");
      pf.addSecondaryYAxis(yAxisHistKey, yAxisHist);
      for (int i = 0; i < datasetHistograms.size(); i++) {
        SimpleHistogramDataset datasetHistogram = datasetHistograms.get(i);
        final int histogramIndex = i;
        XYBarRenderer r = new XYBarRenderer() {
          @Override
          public Paint getSeriesPaint(int series) {
            int colorIndex = histogramIndex % histColors.size();
            return histColors.get(colorIndex);
          }
        };
        StandardXYBarPainter bp = new StandardXYBarPainter();
        r.setBarPainter(bp);
        r.setShadowVisible(false);
        r.setGradientPaintTransformer(null);
        r.setDrawBarOutline(true);
        final String key = String.format("%s-%d", datasetHistKeyBase, i);
        pf.addDataset(key, datasetHistogram, r, yAxisHistKey);
      }
    }

    if (datasetGmms != null) {
      log.debug("Adding GMM datasets");
      final BasicStroke strokeDashed = new BasicStroke(
          1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
          1.0f, new float[]{3.0f, 3.0f}, 0.0f);
      XYLineAndShapeRenderer datasetGmmsRenderer = new XYLineAndShapeRenderer(true, false) {
        @Override
        public Stroke getItemStroke(int row, int column) {
          return strokeDashed;
        }

        @Override
        public Stroke getSeriesStroke(int series) {
          return strokeDashed;
        }

        @Override
        public Paint getSeriesPaint(int series) {
          return COLORS_GMMS[series % COLORS_GMMS.length];
        }
      };
      datasetGmmsRenderer.setDefaultStroke(strokeDashed);
      datasetGmmsRenderer.setDrawSeriesLineAsPath(true);
      pf.addDataset(datasetGmmsKey, datasetGmms, datasetGmmsRenderer, null);
    }

    final CloseOption onClose = cmd.optsPlot.exitOnClose
        ? CloseOption.EXIT
        : CloseOption.DISPOSE;

    // the main call that shows the window with KDE plot and PSM table
    pf.display(onClose, DeltaMassInfo.getNameVersion());
  }
}
