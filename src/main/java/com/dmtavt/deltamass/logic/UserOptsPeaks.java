package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.dmtavt.deltamass.args.converters.DecimalFormatConverter;
import com.dmtavt.deltamass.args.converters.DecimalFormatValidator;
import com.dmtavt.deltamass.args.converters.DecimalFormatWithTostring;
import com.dmtavt.deltamass.args.converters.PathConverter;
import com.dmtavt.deltamass.args.converters.StringToPathValidator;
import java.nio.file.Path;

public class UserOptsPeaks {

  static final int ORDER = 400;
  private static final int EXTRA = ORDER * 1000;

  @Parameter(names = {"-o", "--out"}, required = false,
      description = "If not present prints to STDERR. "
          + "Path to write the output to. If given PATH NOT EXISTS or IS AN EXISINTG "
          + "FILE: writes to that specific file. If given PATH IS AN EXISTING DIRECTORY: creates "
          + "a new file with default name in it.",
      validateWith = StringToPathValidator.class, converter = PathConverter.class,
      order = ORDER)
  public Path out;

  @Parameter(names = {"-w", "--force"}, description = "Overwrite existing output file?")
  public boolean overwrite = false;

  @Parameter(names = {"--min-psms"}, description = "Minimum number of PSMs supporting a peak in "
      + "GMM. Otherwise a peak is not reported.",
      order = ORDER + 10)
  public int minPsmsPerGmm = 10;
  @Parameter(names = {"--min-pct"}, description = "For each nominal delta mass region only report "
      + "peaks that are at least this % of the top peak.",
      order = ORDER + 20)
  public double minPeakPct = 0.025;

  @Parameter(names = {"--digits-mz"}, description = "Number of decimal digits for mass values "
      + "in output.", hidden = true,

      order = EXTRA + 10)
  public int digitsMz = 5;
  @Parameter(names = {"--digits-ab"}, description = "Number of decimal digits for intensity values "
      + "in output.", hidden = true,
      order = EXTRA + 20)
  public int digitsAb = 1;

//  @Parameter(names = {"--out-fmt-mz"}, description = "Format to be used in the output for m/z "
//      + "values. See https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html "
//      + "for details.",
//      validateWith = DecimalFormatValidator.class, converter = DecimalFormatConverter.class,
//      order = EXTRA + 10)
//  public DecimalFormatWithTostring outFormatMz = new DecimalFormatWithTostring("0.#####");
//  @Parameter(names = {"--out-fmt-ab"}, description = "Format for the intensity/abundance in the "
//      + "output file. See also '--out-fmt-mz'.",
//      validateWith = DecimalFormatValidator.class, converter = DecimalFormatConverter.class,
//      order = EXTRA + 20)
//  public DecimalFormatWithTostring outFormatAb = new DecimalFormatWithTostring("0.#E0");
}
