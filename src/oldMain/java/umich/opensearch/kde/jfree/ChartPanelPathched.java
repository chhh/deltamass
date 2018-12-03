package umich.opensearch.kde.jfree;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dmitry Avtonomov
 */
public class ChartPanelPathched extends ChartPanel {

  private static final Logger log = LoggerFactory.getLogger(ChartPanelPathched.class);
  private final ConcurrentHashMap<XYPlot, Range> mapPlotLastState = new ConcurrentHashMap<>(3);
  private JCheckBoxMenuItem checkboxZoomXAxisOnly;
  private JCheckBoxMenuItem checkboxScaleYAxisAuto;
  private Action zoomXonlyAction;
  private Action scaleYautoAction;
  private volatile boolean zoomXonly;
  private volatile boolean scaleYauto;

  public ChartPanelPathched(JFreeChart chart) {
    super(chart);
    init();
  }

  public ChartPanelPathched(JFreeChart chart, boolean useBuffer) {
    super(chart, useBuffer);
    init();
  }

  public ChartPanelPathched(JFreeChart chart, boolean properties, boolean save, boolean print,
      boolean zoom, boolean tooltips) {
    super(chart, properties, save, print, zoom, tooltips);
    init();
  }

  public ChartPanelPathched(JFreeChart chart, int width, int height, int minimumDrawWidth,
      int minimumDrawHeight, int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer,
      boolean properties, boolean save, boolean print, boolean zoom, boolean tooltips) {
    super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth,
        maximumDrawHeight, useBuffer, properties, save, print, zoom, tooltips);
    init();
  }

  public ChartPanelPathched(JFreeChart chart, int width, int height, int minimumDrawWidth,
      int minimumDrawHeight, int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer,
      boolean properties, boolean copy, boolean save, boolean print, boolean zoom,
      boolean tooltips) {
    super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth,
        maximumDrawHeight, useBuffer, properties, copy, save, print, zoom, tooltips);
    init();
  }

  private static Field getField(Class<?> clazz, String fieldName) {
    Class<?> tmpClass = clazz;
    do {
      try {
        Field f = tmpClass.getDeclaredField(fieldName);
        return f;
      } catch (NoSuchFieldException e) {
        tmpClass = tmpClass.getSuperclass();
      }
    } while (tmpClass != null);

    throw new RuntimeException("Field '" + fieldName + "' not found on class " + clazz);
  }

  public static void setRangeAxesAutoscale(JFreeChart chart, boolean rangeAxisAutoscale) {
    Plot plot = chart.getPlot();
    if (plot instanceof XYPlot) {
      XYPlot xyPlot = (XYPlot) plot;
      int rangeAxisCount = xyPlot.getRangeAxisCount();
      for (int i = 0; i < rangeAxisCount; i++) {
        ValueAxis rangeAxis = xyPlot.getRangeAxis(i);
        rangeAxis.setAutoRange(rangeAxisAutoscale);
      }
    }
  }

  @Override
  public void setChart(JFreeChart chart) {
    super.setChart(chart);
    init();
  }

  private void init() {
    setHorizontalAxisTrace(true);
    setVerticalAxisTrace(true);
    setMouseWheelEnabled(true);
    setRangeZoomable(!zoomXonly);

    // ASMS-2017 mods
    setHorizontalAxisTrace(false);
    setVerticalAxisTrace(false);

    if (getChart().getPlot() instanceof XYPlot) {
      XYPlot plot = (XYPlot) getChart().getPlot();
      plot.setDomainPannable(true);
      setRangeAxesAutoscale(getChart(), true);
    }

    getChart().getPlot().addChangeListener(new PlotChangeListener() {
      @Override
      public void plotChanged(PlotChangeEvent event) {
        log.trace("PlotChangeEvent: {} on some {}", event.getType(),
            event.getPlot().getClass().getSimpleName());
        //setRangeAxesAutoscale(getChart(), true);
        final Plot plt = getChart().getPlot();
        if (plt != null && plt instanceof XYPlot) {
          XYPlot plot = (XYPlot) plt;
//                    plot.getRangeAxis().setAutoRange(false);
//                    plot.getRangeAxis().setAutoRange(true);

          if (mapPlotLastState == null) {
            return;
          }
          Range rangeOld = mapPlotLastState.get(plot);
          Range rangeCur = plot.getDomainAxis().getRange();
          if (rangeOld == null) {
            // no messages yet from this plot, add it to the map and
            log.trace("Old range not found, new range created {}", rangeCur);
            mapPlotLastState.put(plot, rangeCur);
            runYAxisRescaling();
          } else {
            if (!rangeOld.equals(rangeCur)) {
              log.trace("Range in map updated, old range {}, new range {}", rangeOld, rangeCur);
              mapPlotLastState.put(plot, rangeCur);
              runYAxisRescaling();
            } else {
              log.trace("Range in map was equal to the new one, nothing to update");
            }
          }
        }

//                if (ChartPanelPathched.this.scaleYauto) {
//                    ChartPanelPathched.this.runYAxisRescaling();
//                }
      }
    });
  }

  private List<Field> getAllFields(Class clazz) {

    List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));

    Class superClazz = clazz.getSuperclass();
    if (superClazz != null) {
      fields.addAll(getAllFields(superClazz));
    }

    return fields;
  }

  @Override
  protected JPopupMenu createPopupMenu(boolean properties, boolean copy, boolean save,
      boolean print, boolean zoom) {
    JPopupMenu menu = super.createPopupMenu(properties, copy, save, print, zoom);
    this.scaleYauto = true;
    this.zoomXonly = true;

    checkboxZoomXAxisOnly = new JCheckBoxMenuItem();
    checkboxZoomXAxisOnly.setSelected(zoomXonly);
    ChartPanelPathched.this.setRangeZoomable(!zoomXonly);
    zoomXonlyAction = new AbstractAction("Zoom X-axis only") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            boolean wasRangeZoomable = ChartPanelPathched.this.isRangeZoomable(true);
            ChartPanelPathched.this.setRangeZoomable(!wasRangeZoomable);
            zoomXonly = !ChartPanelPathched.this.isRangeZoomable(true);
            checkboxZoomXAxisOnly.setSelected(zoomXonly);
          }
        };
        SwingUtilities.invokeLater(runnable);
      }
    };
    checkboxZoomXAxisOnly.setAction(zoomXonlyAction);
    menu.add(checkboxZoomXAxisOnly, 0);

    checkboxScaleYAxisAuto = new JCheckBoxMenuItem();
    checkboxScaleYAxisAuto.setSelected(scaleYauto);
    setRangeAxesAutoscale(getChart(), scaleYauto);
    scaleYautoAction = new AbstractAction("Auto zoom Y-axis") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            scaleYauto = checkboxScaleYAxisAuto.isSelected();
            setRangeAxesAutoscale(getChart(), scaleYauto);
          }
        };
        SwingUtilities.invokeLater(runnable);
      }
    };
    checkboxScaleYAxisAuto.setAction(scaleYautoAction);
    menu.add(checkboxScaleYAxisAuto, 0);

    return menu;
  }

  @Override
  public void restoreAutoBounds() {
    super.restoreAutoBounds();
//        XYPlot plot = getChart().getXYPlot();
//        if (plot.getRangeAxisCount() > 1) {
//            JFreeChartPlot.synchronizeAxes(plot);
//        }
  }

  @Override
  public void zoomInDomain(double x, double y) {
    super.zoomInDomain(x, y);
  }

  @Override
  public void zoomOutDomain(double x, double y) {
    super.zoomOutDomain(x, y);
  }

  @Override
  public void zoomInBoth(double x, double y) {
    super.zoomInBoth(x, y);
  }

  @Override
  public void zoomOutBoth(double x, double y) {
    super.zoomOutBoth(x, y);
  }

  @Override
  public void zoomOutRange(double x, double y) {
    super.zoomOutRange(x, y);
  }

  @Override
  public void zoomInRange(double x, double y) {
    super.zoomInRange(x, y);
  }

  @Override
  public void zoom(Rectangle2D selection) {
    super.zoom(selection);
  }

  private void runYAxisRescaling() {
    if (scaleYauto) {
      log.trace("Running automatic rescaling of Y axis");
      restoreAutoRangeBounds();
    }
  }

  @Override
  public boolean isRangeZoomable() {
    return zoomXonly && scaleYauto;
  }

  public boolean isRangeZoomable(boolean callSuper) {
    return callSuper ? super.isRangeZoomable() : this.isRangeZoomable();
  }
}
