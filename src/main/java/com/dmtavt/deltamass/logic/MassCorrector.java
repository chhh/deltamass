package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.data.PepSearchResult;
import java.io.IOException;

@FunctionalInterface
public interface MassCorrector {
  void apply(PepSearchResult pepSearchResult) throws IOException;
}
