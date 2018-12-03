package umich.opensearch.kde.jfree;

import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.SwingUtils;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.opensearch.kde.api.PepXmlContent;
import umich.opensearch.kde.api.SearchHitDiff;
import umich.opensearch.kde.api.SearchHitDiffs;
import umich.opensearch.kde.gui.KDEOptionsGui;
import umich.ptm.mod.Mod;
import umich.ptm.mod.ModSpecificity;
import umich.ptm.mod.Mods;

/**
 * @author Dmitry Avtonomov
 */
public class JFreeChartPlot {

  private static final Logger log = LoggerFactory.getLogger(JFreeChartPlot.class);
  public double LISTENER_WINDOW_SIZE = 0.01 / 2d;
  public int MAX_PEPS_TO_PRINT = 200;
  JFrame frame = null;
  JFreeChart chart = null;
  ChartPanel panel = null;
  Map<String, RenderingData> datasetMap = new HashMap<>();
  String mainDatasetName = null;
  String title = "";
  InfoWindow infoWnd = null;
  Mods mods;
  List<Marker> permanentMarkers = new ArrayList<>();

  public JFreeChartPlot() {
    init();
  }

  public JFreeChartPlot(String title) {
    this.title = title;
    init();
  }

  public static void synchronizeAxes(XYPlot plot) {
    //T Hackman from here
    // To synchronize on zero and allow the ranges to scale independently
    // lets find and set the first upper axis range

    int rangeAxisCount = plot.getRangeAxisCount();
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

  private void init() {

  }

  public void setMods(Mods mods) {
    this.mods = mods;
  }

  public void customizeChart(JFreeChart chart) {
    setTooltip(chart);
    setPointRenderer(chart);

    // ASMS-2017 mods
    chart.removeLegend();
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(Color.WHITE);
    chart.setTitle((TextTitle) null);
  }

  public void customizePlot(XYPlot plot) {

    Font f = new Font("Calibri", Font.PLAIN, 24);

    ValueAxis xAxis = plot.getDomainAxis();
    xAxis.setLabelFont(f);
    xAxis.setTickLabelFont(f);
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

    ValueAxis yAxis = plot.getRangeAxis();
    yAxis.setLabelFont(f);
    yAxis.setTickLabelFont(f);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

    plot.setBackgroundPaint(Color.WHITE);

    plot.setDomainCrosshairLockedOnData(false);
    plot.setRangeCrosshairLockedOnData(false);

//        Stroke stroke = new BasicStroke(1.0f,
//                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {2.0f, 2.0f}, 0.0f);
//        plot.setDomainCrosshairStroke(stroke);
//        plot.setRangeCrosshairStroke(stroke);
//        plot.setDomainCrosshairVisible(false);
//        plot.setRangeCrosshairVisible(false);
  }

  /**
   * Show the chart.
   *
   * @param whatToDoOnClose use WindowConstants.EXIT_ON_CLOSE if you want to shut down the whole
   * program when the main chart window is closed. or WindowConstants.DISPOSE_ON_CLOSE.
   */
  public void display(int whatToDoOnClose, String frameTitle) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
      e.printStackTrace();
      System.exit(1);
    }

    String title = "KDE on open search mass diffs";
    String xAxisLabel = "Mass Delta";
    String yAxisLabel = "PSM Density";
    RenderingData mainRd = datasetMap.get(mainDatasetName);
    chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, mainRd.dataset);
    customizeChart(chart);
    XYPlot plt = chart.getXYPlot();

    // ASMS-2017 mods
    customizePlot(plt);

    List<RenderingData> primaryYAxisDatasets = new LinkedList<>();
    List<RenderingData> secondaryYAxisDatasets = new ArrayList<>();
    int primaryDatasetIdx = -1;
    for (Map.Entry<String, RenderingData> e : datasetMap.entrySet()) {
      RenderingData rd = e.getValue();
      if (rd.secondaryYAxis) {
        if (rd.name.equals(mainDatasetName)) {
          throw new IllegalStateException(
              "Primary dataset can not be displayed on the secondary Y axis");
        }
        secondaryYAxisDatasets.add(rd);
      } else {
        if (rd.name.equals(mainDatasetName)) {
          primaryDatasetIdx = primaryYAxisDatasets.size();
        }
        primaryYAxisDatasets.add(rd);
      }
    }

    RenderingData mainDataset = primaryYAxisDatasets.remove(primaryDatasetIdx);
    plt.setDataset(0, mainDataset.dataset);
    int primaryYAxisIdx = 0;
    plt.mapDatasetToRangeAxis(0, primaryYAxisIdx);
    int cnt = 1;
    for (int i = 0; i < primaryYAxisDatasets.size(); i++) {
      if (i == primaryDatasetIdx) {
        continue;
      }
      RenderingData rd = primaryYAxisDatasets.get(i);
      plt.setDataset(cnt, rd.dataset);
      plt.setRenderer(cnt, rd.renderer);
      plt.mapDatasetToRangeAxis(cnt, primaryYAxisIdx);
      cnt++;
    }

    if (!secondaryYAxisDatasets.isEmpty()) {

      final NumberAxis yAxis2 = new NumberAxis("Secondary Y Axis");
      int secondaryAxisIdx = 1;
      plt.setRangeAxis(secondaryAxisIdx, yAxis2);

      //XYItemRenderer secondaryRenderer = plt.getRenderer(0);
      //XYLineAndShapeRenderer secondaryRenderer = new XYLineAndShapeRenderer(true, false);
      //secondaryRenderer.setBaseStroke(new BasicStroke(
      //        2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
      //        1.0f, new float[] {6.0f, 6.0f}, 0.0f));

      for (int i = 0; i < secondaryYAxisDatasets.size(); i++) {
        RenderingData rd = secondaryYAxisDatasets.get(i);
        plt.setDataset(cnt, rd.dataset);
        plt.setRenderer(cnt, rd.renderer);
        plt.mapDatasetToRangeAxis(cnt, secondaryAxisIdx);
      }
      synchronizeAxes(plt);
    }

    // first plot secondary data
    plt.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    final ChartPanel chartPanel = new ChartPanelPathched(chart);
    Dimension dims = new Dimension(800, 600);
    chartPanel.setPreferredSize(dims);

    panel = chartPanel;
    frame = new JFrame();
    if (!StringUtils.isBlank(frameTitle)) {
      frame.setTitle(frameTitle);
    }

    frame.setContentPane(chartPanel);
    frame.setDefaultCloseOperation(whatToDoOnClose);
    frame.pack();
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (infoWnd != null) {
          infoWnd.dispatchEvent(new WindowEvent(infoWnd, WindowEvent.WINDOW_CLOSING));
        }
      }

    });
    SwingUtils.centerFrame(frame);
    SwingUtils.setFrameIcons(frame, KDEOptionsGui.ICON_NAMES, KDEOptionsGui.class);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  public void addMouseListener(final java.util.List<PepXmlContent> pepXmls) {
    if (panel != null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          displayInfoWindow();
        }
      });

      ChartMouseListener chartMouseListener = new ChartMouseListener() {
        @Override
        public void chartMouseClicked(final ChartMouseEvent e) {

          switch (e.getTrigger().getButton()) {
            case MouseEvent.BUTTON1:
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  XYPlot xyPlot = JFreeChartPlot.this.chart.getXYPlot();
                  double xVal = xyPlot.getDomainCrosshairValue();
                  double yVal = xyPlot.getRangeCrosshairValue();

                  String clickCoords = String.format("Click at: [%.5f; %.5f]\n", xVal, yVal);
                  double mzLo, mzHi;

                  if (infoWnd != null && infoWnd.isVisible()) {
                    double wndSize = infoWnd.getListenerWindowSize();
                    int maxPepToPrint = infoWnd.getEntriesToShow();
                    mzLo = xVal - wndSize;
                    mzHi = xVal + wndSize;
                    TextConsole out = infoWnd.getTextConsole();
                    out.setText(clickCoords);
                    printModsByMass(out, mzLo, mzHi);
                    printPepsByMass(out, mzLo, pepXmls, mzHi, maxPepToPrint);
                  } else {
                    mzLo = xVal - LISTENER_WINDOW_SIZE;
                    mzHi = xVal + LISTENER_WINDOW_SIZE;
                    System.err.printf(clickCoords);
                    printModsByMass(System.out, mzLo, mzHi);
                    printPepsByMass(System.out, mzLo, pepXmls, mzHi, MAX_PEPS_TO_PRINT);
                  }

                  clearMarkers();
                  Color markerColor = Color.MAGENTA;
                  BasicStroke markerStroke = new BasicStroke(
                      1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                      1.0f, new float[]{10.0f, 10.0f}, 0.0f);
                  Marker mzLoMarker = new ValueMarker(mzLo, markerColor, markerStroke);
                  Marker mzHiMarker = new ValueMarker(mzHi, markerColor, markerStroke);
                  addMarker(mzLoMarker);
                  addMarker(mzHiMarker);
                }
              });
            default:
              return;
          }
        }

        @Override
        public void chartMouseMoved(ChartMouseEvent arg0) {
        }
      };
      panel.addChartMouseListener(chartMouseListener);
    } else {
      System.err.println("You tried to attach a mouse listener before calling .display() method." +
          " ChartPanel was not yet created, so there was nothing to attach a listener to.");
    }
  }

  private void clearMarkers() {
    if (chart == null) {
      throw new IllegalStateException(
          "can't add anything to the chart unless it has been displayed");
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
          "can't add anything to the chart unless it has been displayed");
    }
    XYPlot xyPlot = chart.getXYPlot();
    if (xyPlot == null) {
      return;
    }
    xyPlot.addDomainMarker(marker);
  }

  private void printPepsByMass(Appendable out, double mzLo, List<PepXmlContent> pepXmls,
      double mzHi, int maxPepsToPrint) {
    if (out == null || pepXmls == null) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    int totalFound = 0;
    HashMap<String, AtomicInteger> map = new HashMap<>(500);

    for (PepXmlContent pepXmlContent : pepXmls) {
      List<SearchHitDiffs> hitsList = pepXmlContent.getHitsList();

      // TODO: ACHTUNG: remove this loop
      for (int i = 0; i < hitsList.size(); i++) {
        SearchHitDiffs searchHitDiffs = hitsList.get(i);
        List<SearchHitDiff> hits = searchHitDiffs.getHits();
        for (int j = 0; j < hits.size() - 1; j++) {
          if (hits.get(j).getMassDiffCal() > hits.get(j + 1).getMassDiffCal()) {
            throw new IllegalStateException("Entries are not sorted!");
          }
        }
      }

      for (SearchHitDiffs searchHitDiffs : hitsList) {
        if (!searchHitDiffs.isSorted()) {
          throw new IllegalStateException("Must sort entries first");
        }
        List<SearchHitDiff> hits = searchHitDiffs.getHits();
        Comparator<SearchHitDiff> comparator = new Comparator<SearchHitDiff>() {
          @Override
          public int compare(SearchHitDiff o1, SearchHitDiff o2) {
            return Double.compare(o1.getMassDiff(), o2.getMassDiff());
          }
        };
        SearchHitDiff lo = new SearchHitDiff(0);
        lo.setMassDiff(mzLo);
        int idxLo = Collections.binarySearch(hits, lo, comparator);
        SearchHitDiff hi = new SearchHitDiff(0);
        hi.setMassDiff(mzHi);
        int idxHi = Collections.binarySearch(hits, hi, comparator);

        if (idxLo < 0) {
          idxLo = -idxLo - 1;
        }

        if (idxHi < 0) {
          idxHi = -idxHi - 1;
        }

        List<SearchHitDiff> sublist = hits.subList(idxLo, idxHi);
        totalFound += sublist.size();
        for (SearchHitDiff searchHitDiff : sublist) {
          AtomicInteger count = map.get(searchHitDiff.getSeq());
          if (count == null) {
            map.put(searchHitDiff.getSeq(), new AtomicInteger(1));
          } else {
            count.incrementAndGet();
          }
        }
      }
    }
    sb.append(String
        .format("\n\nTotal %d distinct sequences, %d overall hits\n", map.size(), totalFound));
    int countPrinted = 0;
    for (Map.Entry<String, AtomicInteger> entry : map.entrySet()) {
      sb.append(String.format("\t%s : %d hits\n", entry.getKey(), entry.getValue().get()));
      if (++countPrinted >= maxPepsToPrint) {
        sb.append("\tPeptide list too long, not printing all peptides\n");
        break;
      }
    }

    try {
      out.append(sb.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void printModsByMass(Appendable out, double mzLo, double mzHi) {
    if (mods == null) {
      log.debug("Won't print mod info as mods were not set for this JFReeChart");
    }
    if (out == null || mods == null) {
      return;
    }
    List<Mod> byMass = mods.findByMass(mzLo, mzHi);
    StringBuilder sb = new StringBuilder();

    sb.append(String.format("\n\nMods in mass range [%.4f; %.4f]:\n", mzLo, mzHi));
    if (byMass.isEmpty()) {
      sb.append("\tNone\n");
    } else {
      List<ModEntry> modEntries = new ArrayList<>(byMass.size());

      Collections.sort(byMass, new Comparator<Mod>() {
        @Override
        public int compare(Mod o1, Mod o2) {
          int srcCmp = o1.getRef().getSrc().compareTo(o2.getRef().getSrc());
          if (srcCmp != 0) {
            return srcCmp;
          }
          return o1.getComposition().getMassMono().compareTo(o2.getComposition().getMassMono());
        }
      });

      for (Mod mod : byMass) {

        sb.append("\t");
        sb.append(mod.getRef())
            .append(String.format(" (m=%.5f)", mod.getComposition().getMassMono()));
        List<ModSpecificity> specificities = mod.getSpecificities();
        if (specificities.size() > 0) {
          sb.append(specificities.toString());
        }
        String desc = mod.getDescShort().length() > 80 ? mod.getDescShort().substring(0, 80) + "..."
            : mod.getDescShort();
        sb.append(" ").append(desc);
        sb.append("\n");
      }
    }

    try {
      out.append(sb.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void displayInfoWindow() {
    infoWnd = new InfoWindow("KDE Info Display", frame);
    infoWnd.setEntriesToShow(this.MAX_PEPS_TO_PRINT);
    infoWnd.setListenerWindowSize(this.LISTENER_WINDOW_SIZE);
    infoWnd.setVisible(true);
  }

  protected void setTooltip(JFreeChart chart) {
    XYPlot plot = chart.getXYPlot();
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

    String TOOL_TIP_FORMAT = "{0}: {1} \u21E2 {2}";
    String xFmtStr = "\u0394m/z: 0.000000 ; \u0394m/z: -0.000000";
    String yFmtStr = "#.#####";
    DecimalFormat xFmt = new DecimalFormat(xFmtStr);
    DecimalFormat yFmt = new DecimalFormat(yFmtStr);

    StandardXYToolTipGenerator tipGen = new StandardXYToolTipGenerator(TOOL_TIP_FORMAT, xFmt, yFmt);
    renderer.setBaseToolTipGenerator(tipGen);
    //renderer.setLegendItemToolTipGenerator(new StandardXYSeriesLabelGenerator("Legend {0}"));
  }

  protected void setPointRenderer(JFreeChart chart) {
    XYPlot plot = chart.getXYPlot();
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
//        Ellipse2D.Double pointMarker = new Ellipse2D.Double(-3, -3, 6, 6);
//        renderer.setBaseShape(pointMarker);
//        renderer.setBaseShapesFilled(false);
//        renderer.setBaseShapesVisible(true);
  }

  /**
   * Add a dataset to the plot. If it's the first dataset being added, it will become the main
   * dataset.
   *
   * @param renderer Can be null, in which case a simple line chart will be used.
   * @param secondaryYAxis Set to true if you want this dataset to use the secondary Y axis.
   */
  public void addDataset(String name, XYDataset dataset, XYItemRenderer renderer,
      boolean secondaryYAxis) {
    RenderingData renderingData = new RenderingData(name, dataset, renderer, secondaryYAxis);
    if (datasetMap.isEmpty()) {
      mainDatasetName = name;
    }
    datasetMap.put(name, renderingData);
  }

  /**
   * Add dataset to the plot, that will use some default renderer.
   *
   * @param secondaryYAxis Set to true if you want this dataset to use the secondary Y axis.
   */
  public void addDataset(String name, XYDataset dataset, boolean secondaryYAxis) {
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
//        renderer.setBaseStroke(new BasicStroke(
//                2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
//                1.0f, new float[] {6.0f, 6.0f}, 0.0f));
    addDataset(name, dataset, renderer, secondaryYAxis);
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

  public enum ChartCloseOption {
    DISPOSE(WindowConstants.DISPOSE_ON_CLOSE), EXIT(WindowConstants.EXIT_ON_CLOSE);

    public final int windowConstant;

    ChartCloseOption(int windowConstant) {
      this.windowConstant = windowConstant;
    }
  }

  public static class RenderingData {

    public final String name;
    public final XYDataset dataset;
    public final XYItemRenderer renderer;
    public final boolean secondaryYAxis;


    public RenderingData(String name, XYDataset dataset, XYItemRenderer renderer,
        boolean secondaryYAxis) {
      this.name = name;
      this.dataset = dataset;
      this.renderer = renderer;
      this.secondaryYAxis = secondaryYAxis;
    }
  }

  private class ModEntry {

    final String source;
    final int id;
    final Mod mod;

    private ModEntry(String source, int id, Mod mod) {
      this.source = source;
      this.id = id;
      this.mod = mod;
    }
  }
}
