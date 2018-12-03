package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Creates case-insensitive regular expression.
 */
public class RegexConverter implements IStringConverter<Pattern> {

  @Override
  public Pattern convert(String value) {
    Pattern pattern;
    try {
      pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException e) {
      throw new ParameterException(
          String.format("Invalid regular expression: '%s'", e.getPattern()), e);
    }
    return pattern;
  }
}
