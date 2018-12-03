package com.dmtavt.deltamass.args.converters;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExistingReadablePathValidator implements IValueValidator<List<Path>> {

  @Override
  public void validate(String name, List<Path> value) throws ParameterException {

    for (Path p : value) {
      p = p.toAbsolutePath();
      if (Files.notExists(p)) {
        throw new ParameterException(
            String.format("Parameter: '%s'. Path not exists: '%s'", name, p.toString()));
      }
      if (!Files.isReadable(p)) {
        throw new ParameterException(
            String.format("Parameter: '%s'. Path not readable: '%s'", name, p.toString()));
      }
    }
  }
}
