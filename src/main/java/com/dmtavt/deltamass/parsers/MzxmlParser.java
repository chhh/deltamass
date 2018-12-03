package com.dmtavt.deltamass.parsers;

import java.io.IOException;
import java.nio.file.Path;
import umich.ms.datatypes.LCMSData;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzxml.MZXMLFile;

public class MzxmlParser implements ILcmsParser {
  MZXMLFile lcms;


  public MzxmlParser(Path path) {
    lcms = new MZXMLFile(path.toAbsolutePath().normalize().toString());
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
      return fileNameLowerCase.endsWith(".mzxml");
    }

    @Override
    public MzxmlParser create(Path path) {
      MzxmlParser parser = new MzxmlParser(path);
      parser.lcms.setExcludeEmptyScans(true);
      parser.lcms.setNumThreadsForParsing(Math.min(6, Runtime.getRuntime().availableProcessors()));
      return parser;
    }
  }
}
