package com.dmtavt.deltamass.ui;

import com.dmtavt.deltamass.logic.LogicKde.ProcessingSegment;
import com.dmtavt.deltamass.logic.LogicKde.SpmInfo;
import com.dmtavt.deltamass.messages.MsgModsInRange;
import com.dmtavt.deltamass.messages.MsgPlotClicked;
import com.dmtavt.deltamass.messages.MsgPsmsRequest;
import com.dmtavt.deltamass.messages.MsgPsmsResponse;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.search.BinarySearch;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ptm.PtmFactory;
import umich.ptm.PtmFactory.SOURCE;
import umich.ptm.exceptions.ModParsingException;
import umich.ptm.mod.Mod;
import umich.ptm.mod.Mods;

public class PlotFactory {

  private static final Logger log = LoggerFactory.getLogger(PlotFactory.class);
  private EventBus bus;
  private JFrame frame = null;
  private JFreeChart chart = null;
  private ChartPanel chartPanel = null;
  private Map<String, RenderingData> datasetMap = new LinkedHashMap<>();
  private Map<String, ValueAxis> mapYAxes = new LinkedHashMap<>();
  private String mainDatasetName = null;
  private List<Marker> permanentMarkers = new ArrayList<>();
  private List<ProcessingSegment> data;
  private static Mods mods;

  static {
    try {
      mods = PtmFactory.getMods(EnumSet.of(SOURCE.UNIMOD, SOURCE.GEN));
    } catch (ModParsingException e) {
      throw new RuntimeException("Could not parse/generate PTMs");
    }
  }

  public PlotFactory() {
    init();
    bus.register(this);
  }

  private void init() {
    bus = new EventBus();
  }

  private static void customizeChart(JFreeChart chart) {
    setTooltip(chart);
    setPointRenderer(chart);

    chart.removeLegend();
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(Color.WHITE);
    chart.setTitle((TextTitle) null);
  }

  public void addSecondaryYAxis(String key, ValueAxis axis) {
    mapYAxes.put(key, axis);
  }

  private static void customizePlot(XYPlot plot) {

    //final String fontName = SwingUtils.checkFontAvailable("Fira Code", "Calibri", "Roboto", "Arial");
    final String fontName = SwingUtils.checkFontAvailable("Calibri", "Roboto", "Arial");
    Font f = new Font(fontName, Font.PLAIN, 20);
    Font fAxisLabel = new Font(fontName, Font.PLAIN, 20);
    Font fAxisNumbers = new Font(fontName, Font.PLAIN, 18);

    final int numDomainAxes = plot.getDomainAxisCount();
    for (int i = 0; i < numDomainAxes; i++) {
      final ValueAxis xAxis = plot.getDomainAxis(i);
      xAxis.setLabelFont(fAxisLabel);
      xAxis.setTickLabelFont(fAxisNumbers);
      plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
    }

    final int numRangeAxes = plot.getRangeAxisCount();
    for (int i = 0; i < numRangeAxes; i++) {
      final ValueAxis yAxis = plot.getRangeAxis(i);
      yAxis.setLabelFont(fAxisLabel);
      yAxis.setTickLabelFont(fAxisNumbers);
      plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    }

    plot.setBackgroundPaint(Color.WHITE);

    plot.setDomainCrosshairLockedOnData(false);
    plot.setRangeCrosshairLockedOnData(false);

    Stroke stroke = new BasicStroke(
        1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
        0.0f, new float[]{2.0f, 2.0f}, 0.0f);
    plot.setDomainCrosshairStroke(stroke);
    plot.setRangeCrosshairStroke(stroke);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);
  }

  private static void synchronizeAxes(XYPlot plot) {
    int rangeAxisCount = plot.getRangeAxisCount();
    log.debug("Plot has {} range axes to synchronize", rangeAxisCount);
    List<ValueAxis> axes = new ArrayList<>();
    for (int i = 0; i < rangeAxisCount; i++) {
      axes.add(plot.getRangeAxis(i));
    }
    double[] abs_lb = new double[axes.size()];
    double[] ub = new double[axes.size()];
    double[] upper = new double[axes.size()];
    for (int i = 0; i < axes.size(); i++) {
      ValueAxis ax = axes.get(i);
      abs_lb[i] = Math.abs(ax.getLowerBound());
      ub[i] = ax.getUpperBound();
      upper[i] = (abs_lb[i] > ub[i]) ? abs_lb[i] : ub[i];
      ax.setUpperBound(upper[i]);
    }
    if (axes.stream().allMatch(ax -> ax.getLowerBound() >= 0)) {
      axes.forEach(ax -> ax.setLowerBound(0));
    } else {
      for (int i = 0; i < axes.size(); i++) {
        ValueAxis ax = axes.get(i);
        ax.setLowerBound(-1 * upper[i]);
      }
    }

  }

  private static void synchronizeAxes(XYPlot plot, boolean useOldImplFor2AxesOnly) {
    if (!useOldImplFor2AxesOnly) {
      synchronizeAxes(plot);
      return;
    }
    //T Hackman from here
    // To synchronize on zero and allow the ranges to scale independently
    // lets find and set the first upper axis range

    int rangeAxisCount = plot.getRangeAxisCount();
    log.debug("Plot has {} range axes to synchronize", rangeAxisCount);

    if (rangeAxisCount != 2) {
      throw new IllegalArgumentException(
          "Provided plot had the number of range axes not equal to 2");
    }

    ValueAxis axis1 = plot.getRangeAxis(0);
    ValueAxis axis2 = plot.getRangeAxis(1);

    double abs_axis1lb = Math.abs(axis1.getLowerBound());
    double abs_axis2lb = Math.abs(axis2.getLowerBound());
    double axis1ub = axis1.getUpperBound();
    double axis2ub = axis2.getUpperBound();
    double axis1upper = (abs_axis1lb > axis1ub) ? abs_axis1lb : axis1ub;
    double axis2upper = (abs_axis2lb > axis2ub) ? abs_axis2lb : axis2ub;

    axis1.setUpperBound(axis1upper);
    axis2.setUpperBound(axis2upper);

    // now set the lower range if no negative values set to zero
    if ((axis1.getLowerBound() >= 0) && (axis2.getLowerBound() >= 0)) {
      axis1.setLowerBound(0);
      axis2.setLowerBound(0);
    } else {
      axis1.setLowerBound(-axis1upper);
      axis2.setLowerBound(-axis2upper);
    }

    //T Hackman To here
  }

  public static void setAutoRangeXAxes(XYPlot plot, boolean autorange) {
    int rangeAxisCount = plot.getRangeAxisCount();
    for (int i = 0; i < rangeAxisCount; i++) {
      ValueAxis rangeAxis = plot.getRangeAxis();
      rangeAxis.setAutoRange(autorange);
    }
  }

  private static void setTooltip(JFreeChart chart) {
    XYPlot plot = chart.getXYPlot();
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

    String TOOL_TIP_FORMAT = "{0}: {1} \u21E2 {2}";
    String xFmtStr = "\u0394m/z: 0.000000 ; \u0394m/z: -0.000000";
    String yFmtStr = "#.#####";
    DecimalFormat xFmt = new DecimalFormat(xFmtStr);
    DecimalFormat yFmt = new DecimalFormat(yFmtStr);

    StandardXYToolTipGenerator tipGen = new StandardXYToolTipGenerator(TOOL_TIP_FORMAT, xFmt, yFmt);
    renderer.setDefaultToolTipGenerator(tipGen);
  }

  private static void setPointRenderer(JFreeChart chart) {
    XYPlot plot = chart.getXYPlot();
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
//        Ellipse2D.Double pointMarker = new Ellipse2D.Double(-3, -3, 6, 6);
//        renderer.setBaseShape(pointMarker);
//        renderer.setBaseShapesFilled(false);
//        renderer.setBaseShapesVisible(true);
  }

  /**
   * Show the chart in a separate frame.
   */
  public void display(CloseOption onClose, String frameTitle) {
    log.debug("Plot factory display()");
    SwingUtils.setPlatformLafOrNimbus();
    String chartTitle = "Mass shifts";
    String xAxisLabel = "Mass shift (Da)";
    String yAxisLabel = "PSM Density";
    RenderingData mainRd = datasetMap.get(mainDatasetName);
    chart = ChartFactory.createXYLineChart(chartTitle, xAxisLabel, yAxisLabel, mainRd.dataset);
    customizeChart(chart);
    XYPlot plt = chart.getXYPlot();

    // ASMS-2017 mods
    customizePlot(plt);

    List<RenderingData> primaryYAxisDatasets = new LinkedList<>();
    List<RenderingData> secondaryYAxesDatasets = new ArrayList<>();
    int mainDatasetIdx = -1;
    for (Map.Entry<String, RenderingData> e : datasetMap.entrySet()) {
      RenderingData rd = e.getValue();
      final String yAxisKey = rd.secondaryYAxisKey;
      final boolean isSecondaryAxis = yAxisKey != null;
      if (isSecondaryAxis) {
        if (rd.name.equals(mainDatasetName)) {
          throw new IllegalStateException(
              "Primary dataset can not be displayed on the secondary Y axis");
        }
        if (!mapYAxes.containsKey(yAxisKey)) {
          throw new IllegalStateException(String.format(
              "%s Y axis map does not contain yAxisKey [%s]. Add a new Y axis first.",
              PlotFactory.class.getSimpleName(), yAxisKey));
        }
        secondaryYAxesDatasets.add(rd);
      } else {
        if (rd.name.equals(mainDatasetName)) {
          mainDatasetIdx = primaryYAxisDatasets.size();
        }
        primaryYAxisDatasets.add(rd);
      }
    }

    log.debug("Plotting primary dataset");
    RenderingData mainDataset = primaryYAxisDatasets.remove(mainDatasetIdx);
    plt.setDataset(0, mainDataset.dataset);
    int primaryYAxisIdx = 0;
    plt.mapDatasetToRangeAxis(0, primaryYAxisIdx);
    int cnt = 1;
    for (int i = 0; i < primaryYAxisDatasets.size(); i++) {
      if (i == mainDatasetIdx) {
        continue;
      }
      RenderingData rd = primaryYAxisDatasets.get(i);
      plt.setDataset(cnt, rd.dataset);
      plt.setRenderer(cnt, rd.renderer);
      plt.mapDatasetToRangeAxis(cnt, primaryYAxisIdx);
      cnt++;
    }

    log.debug("Mapping secondary Y Axes");
    final int secondaryAxisIdxStart = 1;
    int secondaryAxisIdxPtr = -1;
    final Map<String, Integer> secondaryAxisIdxMap = new HashMap<>();
    for (String key : mapYAxes.keySet()) {
      secondaryAxisIdxPtr++;
      secondaryAxisIdxMap.put(key, secondaryAxisIdxStart + secondaryAxisIdxPtr);
    }

    log.debug("Checking secondary datasets");
    if (!secondaryYAxesDatasets.isEmpty()) {
      log.debug("Plotting secondary datasets");
      //final NumberAxis yAxis2 = new NumberAxis(secondaryAxisTitle);
      //plt.setRangeAxis(secondaryAxisIdx, yAxis2);

      for (RenderingData rd : secondaryYAxesDatasets) {
        plt.setDataset(cnt, rd.dataset);
        plt.setRenderer(cnt, rd.renderer);
        ValueAxis axis = mapYAxes.get(rd.secondaryYAxisKey);
        Integer axisIdx = secondaryAxisIdxMap.get(rd.secondaryYAxisKey);
        if (plt.getRangeAxis(axisIdx) == null) {
          // if the axis has not yet been added, then add it.
          plt.setRangeAxis(axisIdx, axis);
        }
        plt.mapDatasetToRangeAxis(cnt, axisIdx);
        cnt++;
      }
      log.debug("Synchronizing axes");
      synchronizeAxes(plt);
    }
    log.debug("Chart preparations done");

    // first plot secondary data, then primary on top
    plt.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);

    final ChartPanel chartPanel = new ChartPanelPatched(chart);
    chartPanel.setMinimumSize(new Dimension(100, 100));
    chartPanel.setPreferredSize(new Dimension(800, 600));
    installMouseListener(chartPanel);

    this.chartPanel = chartPanel;
    final String title = StringUtils.isBlank(frameTitle) ? "???" : frameTitle;
    frame = new DeltamassDisplay(this.chartPanel, title, onClose, bus, data);
    frame.pack();
    SwingUtils.centerFrame(frame);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private void installMouseListener(final ChartPanel chartPanel) {
    ChartMouseListener chartMouseListener = new ChartMouseListener() {
      @Override
      public void chartMouseClicked(final ChartMouseEvent e) {

        switch (e.getTrigger().getButton()) {
          case MouseEvent.BUTTON1:
            final XYPlot xyPlot = e.getChart().getXYPlot();
            // These are not set properly when chart crosshairs are not enabled
            final double xVal = xyPlot.getDomainCrosshairValue();
            final double yVal = xyPlot.getRangeCrosshairValue();

            //Point2D p = chartPanel.translateScreenToJava2D(e.getTrigger().getPoint());
            Point2D p = e.getTrigger().getPoint();
            Rectangle2D plotArea = chartPanel.getScreenDataArea();
            XYPlot plot = (XYPlot) chart.getPlot(); // your plot
            final double chartX = plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
            final double chartY = plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());

            SwingUtilities.invokeLater(() -> {
              MsgPlotClicked m = new MsgPlotClicked(chartX, chartY);
              log.debug("Sending message: " + m.toString());
              bus.post(m);
            });
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEvent event) {
        // do nothing
      }
    };

    chartPanel.addChartMouseListener(chartMouseListener);
  }

  private void clearMarkers() {
    if (chart == null) {
      throw new IllegalStateException(
          "can't addUserMark anything to the chart unless it has been displayed");
    }
    XYPlot xyPlot = chart.getXYPlot();
    if (xyPlot == null) {
      return;
    }
    xyPlot.clearDomainMarkers();
  }

  private void addMarker(Marker marker) {
    if (chart == null) {
      throw new IllegalStateException(
          "can't addUserMark anything to the chart unless it has been displayed");
    }
    XYPlot xyPlot = chart.getXYPlot();
    if (xyPlot == null) {
      return;
    }
    xyPlot.addDomainMarker(marker);
  }

  /**
   * Add a dataset to the plot. If it's the first dataset being added, it will become the main
   * dataset.
   *
   * @param renderer Can be null, in which case a simple line chart will be used.
   * @param secondaryYAxisKey Use non-null value if you want this dataset to use the secondary Y
   * axis. The secondary axis must first be added using {@link #addSecondaryYAxis(String,
   * ValueAxis)}.
   */
  public void addDataset(String name, XYDataset dataset, XYItemRenderer renderer,
      String secondaryYAxisKey) {
    RenderingData renderingData = new RenderingData(name, dataset, renderer, secondaryYAxisKey);
    if (datasetMap.isEmpty()) {
      mainDatasetName = name;
    }
    datasetMap.put(name, renderingData);
  }

  /**
   * Add dataset to the plot, that will use some default renderer.
   *
   * @param secondaryYAxisKey Use non-null if you want this dataset to use the secondary Y axis.
   *  The secondary axis must first be added using {@link #addSecondaryYAxis(String, ValueAxis)}.
   */
  public void addDataset(String name, XYDataset dataset, String secondaryYAxisKey) {
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
//        renderer.setBaseStroke(new BasicStroke(
//                2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
//                1.0f, new float[] {6.0f, 6.0f}, 0.0f));
    addDataset(name, dataset, renderer, secondaryYAxisKey);
  }

  public void setMainDataset(String name) {
    if (datasetMap.containsKey(name)) {
      throw new IllegalArgumentException("The dataset mapping did not contain the requested name.");
    }
    mainDatasetName = name;
  }

  public void addPermanentMarker(String datasetKey, String seriesKey,
      Collection<? extends Marker> markers) {
    permanentMarkers.addAll(markers);
  }

  public synchronized XYDataset getDataset(String key) {
    RenderingData rd = datasetMap.get(key);
    return rd == null ? null : rd.dataset;
  }

  public void setData(List<ProcessingSegment> segments) {
    this.data = segments;
  }

  @Subscribe
  public void onRequestPsms(MsgPsmsRequest m) {

    List<Mod> modsInRange = mods.findByMass(m.dmLo, m.dmHi);
    bus.post(new MsgModsInRange(m.dmLo, m.dmHi, modsInRange));

    if (this.data == null || this.data.isEmpty()) {
      return;
    }
    List<List<SpmInfo>> foundSublists = new ArrayList<>();
    for (ProcessingSegment segment : data) {
      final double segDataLo = segment.data.xLoData;
      final double segDataHi = segment.data.xHiData;
      if (!((segDataHi >= m.dmLo) && (segDataLo <= m.dmHi))) {
        continue; // skip segments that don't overlap with target search
      }
      List<SpmInfo> spmInfos = segment.data.spmInfos;
      int searchLo = BinarySearch.search(spmInfos, si -> Double.compare(si.spm.mDiffCorrected, m.dmLo));
      if (searchLo < 0) {
        searchLo = ~searchLo;
      }
      int searchHi = BinarySearch.search(spmInfos, si -> Double.compare(si.spm.mDiffCorrected, m.dmHi));
      if (searchHi < 0) {
        searchHi = ~searchHi;
      } else {
        searchHi += 1; // we will use this as upper bound for .sublist(), the index needs to be exclusive
      }
      if (searchHi - searchLo > 0) {
        foundSublists.add(spmInfos.subList(searchLo, searchHi));
      }
    }

    final int total = foundSublists.stream().mapToInt(List::size).sum();
    if (total == 0) {
      log.info(String.format("No PSMs in dM range: [%.4f, %.4f]", m.dmLo, m.dmHi));
      bus.post(new MsgPsmsResponse(Collections.emptyList()));
      return;
    }

    // DEBUG: START
//    List<SpmInfo> collect = data.stream().flatMap(s -> s.data.spmInfos.stream())
//        .filter(si -> si.spm.mDiffCorrected > m.dmLo && si.spm.mDiffCorrected < m.dmHi)
//        .collect(Collectors.toList());
//    log.error("Streaming size: {}, manual size: {}, {}", collect.size(), total, total == collect.size() ? "Equal" : "Not equal");
    // DEBUG: END


    log.debug("Total {} PSMs in selected area (responding to {} in {})", total,
        MsgPsmsRequest.class.getSimpleName(), PlotFactory.class.getSimpleName());
    final int max = m.softLimit;
    List<SpmInfo> toPost;
    if (total <= max) {
      toPost = copySpmInfosToOneList(foundSublists);
    } else {
      NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
      int choice = JOptionPane.showConfirmDialog(chartPanel,
          String.format("<html>Selection contains %s psms (more than %s limit), truncate?<br/>"
                  + "<ul><li><b>Yes</b> - Truncate to %s</li>"
                  + "<li><b>No</b> - proceed with all %s</li>"
                  + "<li><b>Cancel</b> - to just cancel</li></ul>",
              fmt.format(total), fmt.format(max), fmt.format(max), fmt.format(total)),
          "Large output",
          JOptionPane.YES_NO_CANCEL_OPTION);

      switch (choice) {
        case JOptionPane.YES_OPTION:
          ArrayList<SpmInfo> foundSpmsCut = new ArrayList<>(max);
          for (List<SpmInfo> foundSublist : foundSublists) {
            final int spaceLeft = max - foundSpmsCut.size();
            if (spaceLeft <= 0) {
              break;
            }
            if (spaceLeft >= foundSublist.size()) {
              foundSpmsCut.addAll(foundSublist);
            } else {
              foundSpmsCut.addAll(foundSublist.subList(0, spaceLeft));
            }
          }
          toPost = foundSpmsCut;
          break;
        case JOptionPane.NO_OPTION:
          toPost = copySpmInfosToOneList(foundSublists);
          break;
        case JOptionPane.CANCEL_OPTION:
          toPost = null;
          break;
        default:
          throw new AssertionError("Unexpected response from JOptinoPane selection");
      }
    }
    if (toPost != null) {
      bus.post(new MsgPsmsResponse(toPost));
    }
  }

  private ArrayList<SpmInfo> copySpmInfosToOneList(List<List<SpmInfo>> lists) {
    final int total = lists.stream().mapToInt(List::size).sum();
    ArrayList<SpmInfo> foundSpms = new ArrayList<>(total);
    for (List<SpmInfo> foundSublist : lists) {
      foundSpms.addAll(foundSublist);
    }
    return foundSpms;
  }

  public enum CloseOption {
    DISPOSE(JFrame.DISPOSE_ON_CLOSE), EXIT(JFrame.EXIT_ON_CLOSE);

    public final int windowConstant;

    CloseOption(int windowConstant) {
      this.windowConstant = windowConstant;
    }
  }

  public static class RenderingData {

    public final String name;
    public final XYDataset dataset;
    public final XYItemRenderer renderer;
    public final String secondaryYAxisKey;


    public RenderingData(String name, XYDataset dataset, XYItemRenderer renderer,
        String secondaryYAxisKey) {
      this.name = name;
      this.dataset = dataset;
      this.renderer = renderer;
      this.secondaryYAxisKey = secondaryYAxisKey;
    }
  }

}
