package com.dmtavt.deltamass.parsers;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LcmsParserRegistry {
  private LcmsParserRegistry() {}

  public static final List<IParserFactory<ILcmsParser>> factories = Collections
      .unmodifiableList(Arrays.asList(
          new MzmlParser.Factory(),
          new MzxmlParser.Factory()
      ));

  /**
   * Find a factory in the registry that supports a file.
   * @param path Path to the file.
   * @return Null if no supporting factories found.
   */
  public static IParserFactory<ILcmsParser> find(Path path) {
    String fnLow = path.getFileName().toString().toLowerCase();
    for (IParserFactory<ILcmsParser> factory : factories) {
      if (factory.supports(fnLow, path)) return factory;
    }
    return null;
  }
}
