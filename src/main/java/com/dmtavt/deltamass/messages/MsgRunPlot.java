package com.dmtavt.deltamass.messages;

import com.dmtavt.deltamass.logic.CommandPlot;

public class MsgRunPlot {
  public final Object sender;
  public final CommandPlot cmd;

  public MsgRunPlot(Object sender, CommandPlot cmd) {
    this.sender = sender;
    this.cmd = cmd;
  }
}
