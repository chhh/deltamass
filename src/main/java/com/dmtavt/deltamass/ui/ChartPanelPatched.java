package com.dmtavt.deltamass.ui;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartPanelPatched extends ChartPanel {

  private static final Logger log = LoggerFactory.getLogger(ChartPanelPatched.class);

  private JCheckBoxMenuItem checkboxZoomXAxisOnly;
  private JCheckBoxMenuItem checkboxScaleYAxisAuto;
  private Action zoomXonlyAction;
  private Action scaleYautoAction;
  private volatile boolean zoomXonly;
  private volatile boolean scaleYauto;

  private final ConcurrentHashMap<XYPlot, Range> mapPlotLastState = new ConcurrentHashMap<>(3);

  public ChartPanelPatched(JFreeChart chart) {
    super(chart);
    init();
  }

  @Override
  public void setChart(JFreeChart chart) {
    super.setChart(chart);
    init();
  }

  public ChartPanelPatched(JFreeChart chart, boolean useBuffer) {
    super(chart, useBuffer);
    init();
  }

  public ChartPanelPatched(JFreeChart chart, boolean properties, boolean save, boolean print,
      boolean zoom, boolean tooltips) {
    super(chart, properties, save, print, zoom, tooltips);
    init();
  }

  public ChartPanelPatched(JFreeChart chart, int width, int height, int minimumDrawWidth,
      int minimumDrawHeight, int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer,
      boolean properties, boolean save, boolean print, boolean zoom, boolean tooltips) {
    super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth,
        maximumDrawHeight, useBuffer, properties, save, print, zoom, tooltips);
    init();
  }

  public ChartPanelPatched(JFreeChart chart, int width, int height, int minimumDrawWidth,
      int minimumDrawHeight, int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer,
      boolean properties, boolean copy, boolean save, boolean print, boolean zoom,
      boolean tooltips) {
    super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth,
        maximumDrawHeight, useBuffer, properties, copy, save, print, zoom, tooltips);
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

    getChart().getPlot().addChangeListener(event -> {
      log.trace("PlotChangeEvent: {} on some {}", event.getType(),
          event.getPlot().getClass().getSimpleName());
      //setRangeAxesAutoscale(getChart(), true);
      final Plot plt = getChart().getPlot();
      if (plt instanceof XYPlot) {
        XYPlot plot = (XYPlot) plt;
//        plot.getRangeAxis().setAutoRange(false);
//        plot.getRangeAxis().setAutoRange(true);
        if (mapPlotLastState == null) {
          // Don't trust IDEA's hint that this condition is always true.
          // If you don't believe me, then comment the next line out.
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

//      if (ChartPanelPatched.this.scaleYauto) {
//        ChartPanelPatched.this.runYAxisRescaling();
//      }
    });
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
    ChartPanelPatched.this.setRangeZoomable(!zoomXonly);
    zoomXonlyAction = new AbstractAction("Zoom X-axis only") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Runnable runnable = () -> {
          boolean wasRangeZoomable = ChartPanelPatched.this.isRangeZoomable(true);
          ChartPanelPatched.this.setRangeZoomable(!wasRangeZoomable);
          zoomXonly = !ChartPanelPatched.this.isRangeZoomable(true);
          checkboxZoomXAxisOnly.setSelected(zoomXonly);
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
        Runnable runnable = () -> {
          scaleYauto = checkboxScaleYAxisAuto.isSelected();
          setRangeAxesAutoscale(getChart(), scaleYauto);
        };
        SwingUtilities.invokeLater(runnable);
      }
    };
    checkboxScaleYAxisAuto.setAction(scaleYautoAction);
    menu.add(checkboxScaleYAxisAuto, 0);

    return menu;
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
  public void restoreAutoBounds() {
    super.restoreAutoBounds();
//    XYPlot plot = getChart().getXYPlot();
//    if (plot.getRangeAxisCount() > 1) {
//      PlotFactory.synchronizeAxes(plot);
//    }
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
