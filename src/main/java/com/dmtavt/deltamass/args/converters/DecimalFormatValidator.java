package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import java.text.DecimalFormat;

public class DecimalFormatValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {
    DecimalFormat format = new DecimalFormat();
    try {
      format.applyPattern(value);
    } catch (IllegalArgumentException e) {
      throw new ParameterException(String.format(
          "Parameter '%s' had incorrect format '%s'. " +
              "Check https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html " +
              "for correct formats.", name, value));
    }
  }
}
