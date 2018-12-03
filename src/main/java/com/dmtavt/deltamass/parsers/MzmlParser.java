package com.dmtavt.deltamass.parsers;

import java.io.IOException;
import java.nio.file.Path;
import umich.ms.datatypes.LCMSData;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

public class MzmlParser implements ILcmsParser {
  MZMLFile lcms;


  public MzmlParser(Path path) {
    lcms = new MZMLFile(path.toAbsolutePath().normalize().toString());
  }

  @Override
  public LCMSData parse(LCMSDataSubset subset) throws IOException {
    try {
      LCMSData data = new LCMSData(lcms);
      data.load(subset);
      return data;
    } catch (FileParsingException e) {
      throw new IOException(e);
    }
  }

  public static class Factory implements IParserFactory<ILcmsParser> {

    @Override
    public boolean supports(String fileNameLowerCase, Path path) {
      return fileNameLowerCase.endsWith(".mzml");
    }

    @Override
    public MzmlParser create(Path path) {
      MzmlParser parser = new MzmlParser(path);
      parser.lcms.setExcludeEmptyScans(true);
      parser.lcms.setNumThreadsForParsing(Math.min(4, Runtime.getRuntime().availableProcessors()));
      return parser;
    }
  }
}
