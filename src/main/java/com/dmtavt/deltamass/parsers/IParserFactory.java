package com.dmtavt.deltamass.parsers;

import java.nio.file.Path;

public interface IParserFactory<T> {
  boolean supports(String fileNameLowerCase, Path path);
  T create(Path path);
}
