package com.dmtavt.deltamass.parsers;

import com.dmtavt.deltamass.data.PepSearchFile;
import java.io.IOException;
import java.util.regex.Pattern;

@FunctionalInterface
public interface IPepidParser {
  PepSearchFile parse(final Pattern decoyRegex) throws IOException;
}
