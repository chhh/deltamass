package com.dmtavt.deltamass.args.converters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class DecimalFormatWithTostring extends DecimalFormat {

  public DecimalFormatWithTostring() {
  }

  public DecimalFormatWithTostring(String pattern) {
    super(pattern);
  }

  public DecimalFormatWithTostring(String pattern, DecimalFormatSymbols symbols) {
    super(pattern, symbols);
  }

  @Override
  public String toString() {
    return this.toPattern();
  }
}
