package com.dmtavt.deltamass.logic;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CacheLocator {
  public static final String CACHE_EXT_PEP = ".deltamass-pep-cache";
  public static final String CACHE_EXT_CAL = ".deltamass-cal-cache";

  public static Path locateForPep(Path path) {
    return Paths.get(path.toAbsolutePath().normalize().toString() + CACHE_EXT_PEP);
  }

  /**
   * Locate cached file for a particular pep id file and LCMS file.
   * @param path Path to pep id file.
   * @param rawFileId Identifier of the raw file or experiment within the pep id file. Can be
   *    raw file name or just ordinal number of a search result within the a multi-result
   *    pep id file.
   * @return The path to where the cache file should be. No checks for existence are made.
   */
  public static Path locateForCal(Path path, String rawFileId) {
    return Paths.get(path.toAbsolutePath().normalize().toString() + "_" + rawFileId + CACHE_EXT_CAL);
  }

  public static boolean isPepCacheFile(Path path) {
    return path.getFileName().toString().toLowerCase().endsWith(CACHE_EXT_PEP);
  }

  public static boolean isCalCacheFile(Path path) {
    return path.getFileName().toString().toLowerCase().endsWith(CACHE_EXT_CAL);
  }
}
