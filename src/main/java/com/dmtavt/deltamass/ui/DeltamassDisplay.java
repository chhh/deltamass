package com.dmtavt.deltamass.ui;

import com.dmtavt.deltamass.DeltaMassInfo;
import com.dmtavt.deltamass.logic.LogicKde.ProcessingSegment;
import com.dmtavt.deltamass.logic.LogicKde.SpmInfo;
import com.dmtavt.deltamass.messages.MsgSelectionClear;
import com.dmtavt.deltamass.messages.MsgModsInRange;
import com.dmtavt.deltamass.messages.MsgPlotClicked;
import com.dmtavt.deltamass.messages.MsgPsmsRequest;
import com.dmtavt.deltamass.messages.MsgPsmsResponse;
import com.dmtavt.deltamass.messages.MsgSelectionMade;
import com.dmtavt.deltamass.messages.MsgSwitchView;
import com.dmtavt.deltamass.messages.MsgRequestHistogram;
import com.dmtavt.deltamass.ui.PlotFactory.CloseOption;
import com.dmtavt.deltamass.ui.SimpleTableModel.Col;
import com.dmtavt.deltamass.ui.SimpleTableModel.ColDouble;
import com.dmtavt.deltamass.ui.SimpleTableModel.ColInt;
import com.dmtavt.deltamass.ui.SimpleTableModel.ColString;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.color.ColorHelper;
import com.github.chhh.utils.ser.SwingCachePropsStore;
import com.github.chhh.utils.swing.TextConsole;
import com.github.chhh.utils.swing.UiSpinnerInt;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ptm.mod.Mod;
import umich.ptm.mod.ModSpecificity;

public class DeltamassDisplay extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(DeltamassDisplay.class);

  private static final VIEW DEFAULT_VIEW = VIEW.DETAILED;
  private static final String DELTAMASS_DISPLAY_CACHE = "deltamass-display.cache";

  private final EventBus bus;
  private final SwingCachePropsStore cache;
  private final List<ProcessingSegment> data;

  // chart
  private final ChartPanel chart;
  private final Markers markers;
  // main menu
  private ButtonGroup group;
  private JRadioButtonMenuItem rbViewChart;
  private JRadioButtonMenuItem rbViewDetailed;
  private JSplitPane splitMain;
  // detailed view
  private JSplitPane splitDetailed;
  private JPanel detailedLeftPanel;
  private JPanel detailedRightPanel;
  private TextConsole console;
  private SimpleETable eTable;
  private JTextField statusBar;
  private UiSpinnerInt spinnerPsmLimit;
  private JLabel labelTableCount;
  private volatile VIEW view = VIEW.INIT;
  private JButton btnHistogram;

  public DeltamassDisplay(ChartPanel chart, String frameTitle, CloseOption onClose,
      EventBus bus, List<ProcessingSegment> data) {
    super();
    checkNotNull(chart, "Chart");
    checkNotNull(frameTitle, "Frame title");

    setDefaultCloseOperation(onClose.windowConstant);
    this.setTitle(frameTitle);
    this.bus = bus;
    this.data = data;
    this.cache = new SwingCachePropsStore(DELTAMASS_DISPLAY_CACHE, DeltaMassInfo::getCacheDir, DeltaMassInfo::getNameVersion);
    this.chart = chart;
    this.markers = new Markers();
    this.markers.reset();

    init();
    bus.register(this);
  }

  private void checkNotNull(Object o, String name) {
    if (o == null) {
      throw new NullPointerException(name + " can't be null");
    }
  }

  private void init() {
    SwingUtils.setFrameIcons(this, DeltaMassUiElements.ICON_NAMES, DeltaMassUiElements.class);
    this.setJMenuBar(createMenuBar());
    setView(DEFAULT_VIEW);
    chart.setDoubleBuffered(true);

    cache.load(this);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        log.debug("Closing Display");
        cache.store(DeltamassDisplay.this);
      }

      @Override
      public void windowClosed(WindowEvent e) {
        log.debug("Closed Display");
      }
    });
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onSwitchView(MsgSwitchView m) {
    setView(m.view);
  }

  @Subscribe
  public void onRequestHistogram(MsgRequestHistogram m) {
    // Dataset histogram
    List<SimpleHistogramDataset> datasetHistograms = new ArrayList<>();
    final boolean doHistogram = false;
    log.debug("Dataset Histogram");
    List<Double> histDx = Arrays.asList(0.002, 0.001, 0.0005);
    histDx.sort((o1, o2) -> Double.compare(o2,o1));
    final List<ProcessingSegment> segments = this.data;
    for (Double dx : histDx) {
//      double histLo = Double.POSITIVE_INFINITY;
//      double histHi = Double.NEGATIVE_INFINITY;
//      for (ProcessingSegment seg : segments) {
//        histLo = Math.min(histLo, seg.data.xLoBracket);
//        histHi = Math.max(histHi, seg.data.xHiBracket);
//      }
      double histLo = m.mLo;
      double histHi = m.mHi;

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
      log.debug("Create dataset histogram key: {}, total num data points: {}, step: {}, offset: {}", key, totalDataPoints, dx, offset);
      datasetHistograms.add(hist);
    }


    final String datasetHistKeyBase = "dataset-Histogram";
    final List<Color> palette = ColorHelper
        .getDistinctColors(datasetHistograms.size(), 0.5f, 0.75f);
    final List<Color> histColors = new ArrayList<>();
    for (Color c : palette) {
      final int histAlpha = 100;
      Color cTransparent = new Color(c.getRed(), c.getGreen(), c.getBlue(), histAlpha);
      histColors.add(cTransparent);
    }

    final String yAxisHistKey = "histogram";
    final String yAxisHistLabel = "Count (histogram)";
    ValueAxis yAxisHist = new NumberAxis(yAxisHistLabel);

    // adding histogram datasets
    XYPlot plt = getPlot(chart);
    for (int i = 0; i < datasetHistograms.size(); i++) {
      SimpleHistogramDataset datasetHistogram = datasetHistograms.get(i);
      datasetHistogram.setGroup(new DatasetGroup(yAxisHistKey));
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

      // add dataset to plot
      final int datasetIdx = plt.getDatasetCount();
      plt.setRenderer(datasetIdx, r);

      int existingYAxisIdx = -1;
      for (int j = 0; j < plt.getRangeAxisCount(); j++) {
        if (yAxisHistLabel.equals(plt.getRangeAxis(j).getLabel())) {
          existingYAxisIdx = j;
          break;
        }
      }
      ValueAxis yAxis;
      if (existingYAxisIdx == -1) {
        // axis not exists
        yAxis = new NumberAxis(yAxisHistLabel);
        existingYAxisIdx = plt.getRangeAxisCount();
        plt.setRangeAxis(existingYAxisIdx, yAxis);
      } else {
        yAxis = plt.getRangeAxis(existingYAxisIdx);
      }
      plt.mapDatasetToRangeAxis(datasetIdx, existingYAxisIdx);
      plt.setDataset(datasetIdx, datasetHistogram);
    }

  }

  @Subscribe
  public void onModsInRange(MsgModsInRange m) {
    log.debug("{} received {} message: {}",
        DeltamassDisplay.class.getSimpleName(), MsgModsInRange.class.getSimpleName(), m.toString());
    console.setText("");
    List<String> lines = new ArrayList<>();

    lines.add(String.format("Mass shift range: [%.4f, %.4f], spans [%.4f] Da",
        m.dmLo, m.dmHi, Math.abs(m.dmHi - m.dmLo)));
    lines.add("");

    if (m.mods == null || m.mods.isEmpty()) {
      lines.add("No known modifications");

    } else {
      int maxRefLen = m.mods.stream().mapToInt(mod -> mod.getRef().toString().length())
          .max().orElse(10);
      StringBuilder fmt = new StringBuilder();
      fmt.append("%+4.4f [%-").append(maxRefLen).append("s] %s\nDesc: %s\nSites: %s");

      for (Mod mod : m.mods) {
        String sites = mod.getSpecificities().stream().map(ModSpecificity::toString)
            .collect(Collectors.joining(", "));
        Double massMono = mod.getComposition().getMassMono();
        lines.add(String
            .format(fmt.toString(), massMono, mod.getRef(), mod.getDescShort(), mod.getDescLong(),
                sites));
      }
    }
    console.setText(String.join("\n----------\n", lines));

    // add the mods as markers to the view
    if (m.mods != null) {
      List<Double> listOfUniqueMasses = m.mods.stream().mapToDouble(mod -> mod.getComposition().getMassMono())
          .filter(Double::isFinite).distinct().boxed().collect(Collectors.toList());
      for (Double modMass : listOfUniqueMasses) {
        markers.addModMark(modMass);
      }
    }
  }

  @Subscribe
  public void onResponsePsms(MsgPsmsResponse m) {
    log.debug("{} received {} PSMs in {} message", DeltamassDisplay.class.getSimpleName(),
        m.spmInfos.size(), MsgPsmsResponse.class.getSimpleName());

    SimpleTableModel<SpmInfo> tableModel = new SimpleTableModel<>(m.spmInfos, createTableModelColumns(m.spmInfos));
    log.debug("Setting new model for table with {} rows, {} cols", tableModel.data.size(), tableModel.cols.size());
    eTable.setModel(tableModel);
    labelTableCount.setText(String.format("%d PSMs in selection", tableModel.data.size()));
  }

  private List<Col<SpmInfo, ?>> createTableModelColumns(List<SpmInfo> spms) {
    List<Col<SpmInfo, ?>> cols = new ArrayList<>();
    cols.add(new ColString<>(si -> si.spm.seq, "Seq"));
    cols.add(new ColString<>(si -> si.spm.mods, "Mods"));
    cols.add(new ColString<>(si -> si.psr.proteinAccessions.get(si.spm.protId), "Prot"));
    cols.add(new ColDouble<>(si -> si.spm.mDiffCorrected, "dM (Corr)"));
    cols.add(new ColDouble<>(si -> si.spm.mDiff, "dM (raw)"));
    cols.add(new ColInt<>(si -> si.spm.charge, "z"));
    cols.add(new ColDouble<>(si -> si.spm.mzObs, "m/z (Obs)"));
    cols.add(new ColDouble<>(si -> si.spm.mObsNeutral, "M (Obs, z=0)"));
    cols.add(new ColDouble<>(si -> si.spm.mCalcNeutral, "M (Calc, z=0)"));
    cols.add(new ColDouble<>(si -> si.spm.rtSec, "RT (sec"));
    cols.add(new ColString<>(si -> si.psr.rawFileName, "File"));

    if (spms != null && !spms.isEmpty()) {
      List<String> scoreNames = spms.stream()
          .map(si -> si.psr).distinct()
          .flatMap(psr -> psr.scoreMapping.keySet().stream()).distinct()
          .collect(Collectors.toList());
      for (String scoreName : scoreNames) {
        cols.add(new ColDouble<>(si -> {
          Integer scoreIdx = si.psr.scoreMapping.get(scoreName);
          if (scoreIdx == null) {
            return Double.NaN;
          }
          return si.spm.scores[scoreIdx];
        }, scoreName));
      }
    }

//    cols.add(new ColDouble<>(si -> si., ""));
    return cols;
  }

  private void detailedViewTeardown() {
    statusBar = null;
    if (console != null) {
      console.setText("");
    }
    console = null;

    if (eTable != null) {
      eTable.clearSelection();
      eTable.setModel(new DefaultTableModel());
    }
    eTable = null;
    spinnerPsmLimit = null;
    labelTableCount = null;

    detailedLeftPanel = null;
    detailedRightPanel = null;
    splitDetailed = null;

    splitMain = null;
  }

  private void detailedViewSetup() {

    splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitDetailed = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    Dimension minSize100x100 = new Dimension(100, 100);
    chart.setMinimumSize(minSize100x100);

    // left part of detailed view
    splitDetailed.add(createDetailedLeftPanel());

    // right part of detailed view
    splitDetailed.add(createDetailedRightPanel());

    splitDetailed.setMinimumSize(minSize100x100);
    splitMain.add(chart);
    splitMain.add(splitDetailed);

    splitDetailed.revalidate();
    splitDetailed.resetToPreferredSizes();
    splitMain.revalidate();
    splitMain.resetToPreferredSizes();
  }

  private JPanel createDetailedLeftPanel() {

    // top  status bar text-field
    JPanel topPanel = new JPanel(new GridBagLayout());
    statusBar = new JTextField();
    statusBar.setEditable(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(statusBar, gbc);

    // text console
    JScrollPane scrollConsole = new JScrollPane();
    Dimension minSize50x10 = new Dimension(50, 10);
    scrollConsole.setMinimumSize(minSize50x10);
    console = new TextConsole();
    console.setMinimumSize(minSize50x10);
    scrollConsole.setViewportView(console);

    detailedLeftPanel = new JPanel(new BorderLayout());
    detailedLeftPanel.setMinimumSize(minSize50x10);
    detailedLeftPanel.add(topPanel, BorderLayout.NORTH);
    detailedLeftPanel.add(scrollConsole, BorderLayout.CENTER);

    return detailedLeftPanel;
  }

  private JPanel createDetailedRightPanel() {
    Dimension minSize = new Dimension(100, 100);

    // table/PSM controls
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    controls.setBorder(new TitledBorder("PSM list controls"));
    controls.add(new JBtnBuilder().text("Clear Selection").tip("Clear highlighted range")
        .onClick(e -> bus.post(new MsgSelectionClear())).create());

    btnHistogram = new JBtnBuilder().text("Histogram").tip("Show histogram for current view")
        .enabled(false)
        .onClick(e -> {
          //ValueAxis xAxis = getPlot(chart).getDomainAxis();
          //double lb = xAxis.getLowerBound();
          //double ub = xAxis.getUpperBound();

          double[] sel = markers.getCurrentSelection();
          if (sel == null) {
            log.error("Selection was null at histogram request");
            return;
          }
          if (sel.length != 2) {
            log.error("Selection array length !=2 at histogram creation");
            return;
          }

          final double maxHistSpanWarning = 3.0;
          final double distance = Math.abs(sel[0] - sel[1]);
          if (distance > maxHistSpanWarning) {
            JOptionPane.showConfirmDialog(this,
                String.format("<html>Histogram span is larger than %.1f Da.<br/>"
                        + "It <b>might take a while</b> to plot.<br/>"
                        + "Do you want to proceed?",
                    maxHistSpanWarning));
          }

          bus.post(new MsgRequestHistogram(sel[0], sel[1]));
        }).create();

    controls.add(btnHistogram);
    spinnerPsmLimit = new UiSpinnerInt(10000, 0, Integer.MAX_VALUE, 5000);
    spinnerPsmLimit.setPreferredSize(new Dimension(150, 20));
    FormEntry feLimit = new FormEntry("view.psm-limit", "Limit", spinnerPsmLimit);
    controls.add(Box.createHorizontalStrut(3));
    controls.add(feLimit.label());
    controls.add(feLimit.comp);

    labelTableCount = new JLabel();
    controls.add(labelTableCount);

    // PSM table
    JScrollPane scroll = new JScrollPane();
    scroll.setMinimumSize(minSize);
    scroll.setMinimumSize(minSize);
    eTable = new SimpleETable();
    eTable.setMinimumSize(minSize);
    scroll.setViewportView(eTable);

    // whole panel
    detailedRightPanel = new JPanel(new BorderLayout());
    detailedRightPanel.setMinimumSize(minSize);
    detailedRightPanel.add(controls, BorderLayout.NORTH);
    detailedRightPanel.add(scroll, BorderLayout.CENTER);

    return detailedRightPanel;
  }

  private synchronized void setView(VIEW v) {
    log.debug("setView() called with view: " + v.toString());
    if (this.view == v) {
      log.debug("Same as old view, not switching: " + v.toString());
      return;
    }

    view = v;
    getContentPane().removeAll();
    detailedViewTeardown();
    group.clearSelection();

    switch (v) {
      case INIT:
        getContentPane().setLayout(new BorderLayout());
        getContentPane()
            .add(new JLabel("Initializing", SwingConstants.CENTER), BorderLayout.CENTER);
        break;
      case CHART:
        group.setSelected(rbViewChart.getModel(), true);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(chart, BorderLayout.CENTER);
        break;
      case DETAILED:
        group.setSelected(rbViewDetailed.getModel(), true);
        getContentPane().setLayout(new BorderLayout());
        detailedViewSetup();
        getContentPane().add(splitMain, BorderLayout.CENTER);

        break;
      default:
        throw new AssertionError("Unknown enum constant");
    }

    this.pack();
    this.revalidate();
    if (splitDetailed != null) {
      splitDetailed.setDividerLocation(0.3);
    }
    if (splitMain != null) {
      splitMain.setDividerLocation(0.5);
    }
  }

  private JMenuBar createMenuBar() {
    final JMenuBar bar = new JMenuBar();
    JMenu menuFile = new JMenu("File");
    JMenu menuView = new JMenu("View");
    JMenu menuActions = new JMenu("Actions");

    JMenuItem close = new JMenuItem("Close Window");
    close.addActionListener(
        e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    menuFile.add(close);

    menuView.addSeparator();
    group = new ButtonGroup();
    rbViewChart = new JRadioButtonMenuItem("Only plot");
    rbViewChart.addActionListener(e -> bus.post(new MsgSwitchView(VIEW.CHART)));
    rbViewChart.setSelected(false);
    //rbMenuItem.setMnemonic(KeyEvent.VK_R);
    group.add(rbViewChart);
    menuView.add(rbViewChart);

    rbViewDetailed = new JRadioButtonMenuItem("Detailed");
    rbViewDetailed.addActionListener(e -> bus.post(new MsgSwitchView(VIEW.DETAILED)));
    //rbMenuItem.setMnemonic(KeyEvent.VK_O);
    rbViewDetailed.setSelected(false);

    group.clearSelection();

    group.add(rbViewDetailed);
    menuView.add(rbViewDetailed);
    menuView.addSeparator();

    JMenuItem itemClearMarkers = new JMenuItem("Clear markers");
    itemClearMarkers.addActionListener(e -> bus.post(new MsgSelectionClear()));
    menuActions.add(itemClearMarkers);

    bar.add(menuFile);
    bar.add(menuView);
    bar.add(menuActions);

    return bar;
  }

  @Subscribe
  public void onPlotClicked(MsgPlotClicked m) {
    log.debug("onPlotClicked(MsgPlotClicked m) fired, message: " + m.toString());
    markers.addUserMark(m.x);
    if (statusBar != null) {
      statusBar.setText(String.format("Click at dm: %.5f", m.x));
    }
  }

  @Subscribe
  public void onSelectionMade(MsgSelectionMade m) {
    btnHistogram.setEnabled(true);
  }

  @Subscribe
  public void onClearMarkers(MsgSelectionClear m) {
    markers.reset();
    eTable.setModel(new SimpleTableModel<>(Collections.emptyList(), createTableModelColumns(Collections.emptyList())));
    labelTableCount.setText("No selection");
    console.setText("");

    // clear histogram
    btnHistogram.setEnabled(false);
    XYPlot plt = getPlot(chart);
    final int datasetCount = plt.getDatasetCount();
    for (int i = 0; i < datasetCount; i++) {
      XYDataset dataset = plt.getDataset(i);
      if (dataset == null) {
        continue;
      }
      DatasetGroup group = dataset.getGroup();
      if (group == null) {
        continue;
      }
      if (group.getID() != null && group.getID().toLowerCase().startsWith("histogram")) {
        plt.setDataset(i, null);
      }
    }
  }

  private XYPlot getPlot(ChartPanel chartPanel) {
    if (chartPanel == null) {
      return null;
    }
    final JFreeChart jfc = chartPanel.getChart();
    if (jfc == null) {
      return null;
    }
    return jfc.getXYPlot();
  }

  public enum VIEW {INIT, CHART, DETAILED}

  private class Markers {

    final private List<ValueMarker> userMarks = new ArrayList<>();
    final private List<IntervalMarker> areaMarks = new ArrayList<>();
    final private List<ValueMarker> modMarks = new ArrayList<>();
    final private Color USER_MARKER_COLOR = Color.MAGENTA;
    final private BasicStroke USER_MARKER_STROKE = new BasicStroke(
        1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
        1.0f, new float[]{10.0f, 10.0f}, 0.0f);


    void addModMark(double xVal) {
      BasicStroke stroke = new BasicStroke(
          1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL,
          1.0f, new float[]{5.0f, 5.0f}, 0.0f);
      Color color = new Color(42, 103, 109);
      ValueMarker vm = new ValueMarker(xVal, color, stroke);
      modMarks.add(vm);
      getPlot(chart).addDomainMarker(vm);
    }

    void addUserMark(double xVal) {
      if (areaMarks.size() >= 1) {
        log.info("Clear previous selection before adding new ones.");
        return;
      }
      if (userMarks.size() == 0) {
        // adding first user mark
        final ValueMarker m = new ValueMarker(xVal, USER_MARKER_COLOR, USER_MARKER_STROKE);
        userMarks.add(m);
        getPlot(chart).addDomainMarker(m);

      } else {
        // create an area mark
        final ValueMarker vm = new ValueMarker(xVal, USER_MARKER_COLOR, USER_MARKER_STROKE);
        userMarks.add(vm);
        userMarks.sort(Comparator.comparingDouble(ValueMarker::getValue));
        final double lo = userMarks.get(0).getValue();
        final double hi = userMarks.get(1).getValue();
        XYPlot plot = getPlot(chart);
        userMarks.forEach(plot::removeDomainMarker);
        addRangeMark(lo, hi);
        bus.post(new MsgSelectionMade(lo , hi));

        log.info(
            String.format("Requesting PSM info for dM range [%.4f, %.4f]", lo, hi));
        bus.post(new MsgPsmsRequest(lo, hi, spinnerPsmLimit.getActualValue()));

      }
    }

    public double[] getCurrentSelection() {
      if (areaMarks.isEmpty()) {
        return null;
      }
      double[] sel = new double[2];
      sel[0] = areaMarks.get(0).getStartValue();
      sel[1] = areaMarks.get(0).getEndValue();
      return sel;
    }

    void addRangeMark(double xValLo, double xValHi) {
      Color color = new Color(175, 123, 43, 44);
      IntervalMarker im = new IntervalMarker(xValLo, xValHi, color);
      areaMarks.add(im);
      getPlot(chart).addDomainMarker(im);
    }

    void reset() {
      XYPlot plot = getPlot(chart);
      plot.clearDomainMarkers();

      userMarks.clear();
      areaMarks.clear();
      modMarks.clear();
    }
  }

  private class JBtnBuilder {

    private JButton b = new JButton();

    public JBtnBuilder text(String text) {
      b.setText(text);
      return this;
    }

    public JBtnBuilder onClick(ActionListener actionListener) {
      b.addActionListener(actionListener);
      return this;
    }

    public JBtnBuilder tip(String text) {
      b.setToolTipText(text);
      return this;
    }

    public JBtnBuilder enabled(boolean enabled) {
      b.setEnabled(enabled);
      return this;
    }



    public JButton create() {
      return b;
    }
  }
}
