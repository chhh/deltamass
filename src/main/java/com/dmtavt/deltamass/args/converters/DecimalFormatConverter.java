package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import java.text.DecimalFormat;

public class DecimalFormatConverter implements IStringConverter<DecimalFormat> {

  @Override
  public DecimalFormat convert(String value) {

    DecimalFormat format;
    try {
      format = new DecimalFormat(value);
    } catch (Exception e) {
      throw new ParameterException(String.format(
          "Invalid format '%s'. Check https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html "
              +
              "for correct formats.", value));
    }
    return format;
  }
}
