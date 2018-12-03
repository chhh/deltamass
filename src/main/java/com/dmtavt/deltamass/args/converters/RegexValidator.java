package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {
    try {
      Pattern.compile(value);
    } catch (PatternSyntaxException e) {
      throw new ParameterException(
          String.format("Invalid regular expression: '%s'", e.getPattern()), e);
    }
  }
}
