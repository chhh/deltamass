package com.dmtavt.deltamass.utils;

public class NumUtils {
  private NumUtils() {}

  public static boolean isGoodDouble(Double v) {
    return v != null && !Double.isNaN(v) && Double.isFinite(v);
  }
}
