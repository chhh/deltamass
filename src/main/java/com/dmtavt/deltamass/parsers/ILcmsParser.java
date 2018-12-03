package com.dmtavt.deltamass.parsers;

import java.io.IOException;
import umich.ms.datatypes.LCMSData;
import umich.ms.datatypes.LCMSDataSubset;

@FunctionalInterface
public interface ILcmsParser {
  LCMSData parse(LCMSDataSubset subset) throws IOException;
}
