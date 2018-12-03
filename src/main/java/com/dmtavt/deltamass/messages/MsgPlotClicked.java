package com.dmtavt.deltamass.messages;

import java.text.DecimalFormat;

public class MsgPlotClicked {
  public final double x;
  public final double y;
  final private static DecimalFormat df = new DecimalFormat("0.00");

  public MsgPlotClicked(double x, double y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public String toString() {
    return "MsgPlotClicked{" +
        "x=" + df.format(x) +
        ", y=" + df.format(y) +
        '}';
  }
}
