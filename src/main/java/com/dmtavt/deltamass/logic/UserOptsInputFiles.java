package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.dmtavt.deltamass.args.DecoyTreatment;
import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.args.converters.ExistingReadablePathValidator;
import com.dmtavt.deltamass.args.converters.PathConverter;
import com.dmtavt.deltamass.args.converters.RegexConverter;
import com.dmtavt.deltamass.args.converters.RegexValidator;
import com.dmtavt.deltamass.args.converters.StringToPathValidator;
import com.dmtavt.deltamass.predicates.SpmPredicateFactory;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class UserOptsInputFiles {

  private static final int ORDER = 100;
  private static final int EXTRA = ORDER * 1000;

  @Parameter(names = {"-i", "--in"}, required = true,
      description = "Relative or absolute paths to directories or separate files. "
          + "Separate entries with commas.",
      validateWith = StringToPathValidator.class, converter = PathConverter.class,
      validateValueWith = ExistingReadablePathValidator.class, splitter = CommaParameterSplitter.class,
      order = ORDER + 10)
  public List<Path> inputFiles;

  @Parameter(names = {"--no-recurse"}, description = "Do not recurse into sub-directories.",
      order = ORDER + 11)
  public boolean noRecurse = false;

  @Parameter(names = {"--file-regex"},
      description = "Regular expression that input files will be matched against. If not set, "
          + "will try to accept all supported files.",
      validateWith = RegexValidator.class, converter = RegexConverter.class,
      order = ORDER + 20)
  public Pattern fileRegex = null; //Pattern.compile(".*\\.pep\\.?xml$");

  @Parameter(names = {"--decoy-regex"},
      description = "Regex to identify decoys. Matched against protein ID string.",
      validateWith = RegexValidator.class, converter = RegexConverter.class, order = ORDER + 30)
  public Pattern decoyRegex = Pattern.compile("^rev_.*");

  @Parameter(names = {"--decoys"}, description = "What to do with decoys.", order = ORDER + 31)
  public DecoyTreatment decoyTreatment = DecoyTreatment.FORWARDS_ONLY;

  @Parameter(names = {"-f", "--filter"}, description =
      "Predicates for filtering incoming PSMs. E.g. " +
          "\"expect<=0.001\" or \"peptideprophet>0.95\" for pepxml or \"Mascot:score>1.8\" for mzid.", order =
      ORDER + 40)
  public List<SpmPredicateFactory> predicates;

  @Parameter(names = {"--mLo"}, description = "Low mass cutoff.", order = ORDER + 58)
  public Double mLo = null;
  @Parameter(names = {"--mHi"}, description = "High mass cutoff.", order = ORDER + 59)
  public Double mHi = null;

  @Parameter(names = {"-x", "--exclude"}, arity = 2, description =
      "Exclude ranges of mass deltas from analysis. " +
          "To specify multiple ranges: \"-x -0.5 0.5 -x 2.5 3.5\"", order = ORDER + 60)
  public List<Double> excludeMzRanges = Arrays.asList(-0.5, 0.5);

  @Parameter(names = {"-c", "--calibration"}, description =
      "Correct observed peptide masses. For PEP_ID the " +
          "original LCMS files are required.", order = ORDER + 70)
  public MassCorrection massCorrection = MassCorrection.ZERO_PEAK;

  @Parameter(names = {"-s", "--search-path"}, description = "Additional search paths for raw LCMS "
      + "files. Used only with '--calibration PEP_ID'. Separate entries with commas.",
      validateWith = StringToPathValidator.class, converter = PathConverter.class,
      validateValueWith = ExistingReadablePathValidator.class, splitter = CommaParameterSplitter.class,
      order = ORDER + 91)
  public List<Path> additionalSearchPaths = Collections.emptyList();

  @Parameter(names = {"--score"}, description = "When '--calibration PEP_ID' is used, this score "
      + "is used for FDR estimation, to select good calibration peptides.")
  public String scoreNameForPsmSorting = "expect";

  @Parameter(names = {"--nc", "--no-cache"}, description =
      "Do not cache the parsed content of input files. The default setting greatly speeds up "
          + "consecutive analysis of the same input files.", order =
      EXTRA + 80)
  public boolean noCache = false;
}
