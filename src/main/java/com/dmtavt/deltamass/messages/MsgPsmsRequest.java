package com.dmtavt.deltamass.messages;

public class MsgPsmsRequest {
  public final double dmLo;
  public final double dmHi;
  public final int softLimit;

  public MsgPsmsRequest(double dmLo, double dmHi, int softLimit) {
    this.dmLo = dmLo;
    this.dmHi = dmHi;
    this.softLimit = softLimit;
  }
}
