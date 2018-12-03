package com.dmtavt.deltamass.messages;

public class MsgSelectionMade {

  private final double lo;
  private final double hi;

  public MsgSelectionMade(double lo, double hi) {
    this.lo = lo;
    this.hi = hi;
  }
}
