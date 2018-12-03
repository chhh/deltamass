package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IStringConverter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convert string to {@link Path}.
 */
public class PathConverter implements IStringConverter<Path> {

  @Override
  public Path convert(String value) {
    return Paths.get(value).toAbsolutePath();
  }
}
