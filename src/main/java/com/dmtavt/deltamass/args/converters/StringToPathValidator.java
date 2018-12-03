package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StringToPathValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {
    Path path;
    try {
      path = Paths.get(value);
    } catch (InvalidPathException e) {
      throw new ParameterException(
          String.format("Parameter: '%s'. Can't parse path: '%s'", name, value));
    }
  }
}
