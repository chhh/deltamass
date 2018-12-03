package umich.opensearch.kde;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.CommaParameterSplitter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.opensearch.kde.params.*;
import umich.opensearch.kde.params.denoise.Denoising;
import umich.opensearch.kde.params.predicates.MassDiffRangePredicate;
import umich.opensearch.kde.params.predicates.ScorePredicate;
import umich.opensearch.kde.pepxml.DecoyDetector;
import umich.opensearch.kde.pepxml.DecoyDetectorAcceptAll;
import umich.opensearch.kde.pepxml.DecoyDetectorByProtName;
import umich.opensearch.kde.util.DecimalFormatWithToString;

/**
 * @author Dmitry Avtonomov
 */
public class OpenSearchParams implements Serializable {

  public static final String APP_NAME = "Open Search KDE";
  public static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Logger log = LoggerFactory.getLogger(OpenSearchParams.class);
  private static final long serialVersionUID = -5354151314716159125L;
  @Parameter(names = {"-i", "--in"}, description =
      "Relative or absolute path to input file. Can be directories or separate file paths." +
          " Separate entries with commas.",
      required = true, validateWith = PathListParameter.class, converter = PathParameter.class, splitter = CommaParameterSplitter.class)
  protected List<Path> inFilePath;
  @Parameter(names = {"-ip",
      "--in_pattern"}, description = "Regular expression that files will be matched against.")
  protected String inFileRegex = ".*\\.pep\\.xml";
  @Parameter(names = {"--symlinks"}, arity = 1, hidden = true,
      description = "Try to follow symlinks.")
  protected boolean followSymlinks = true;
  @Parameter(names = {"--help", "-help"}, description = "Display help message", help = true)
  protected Boolean help;
  @Parameter(names = {"-d", "--decoy_prefix"}, description =
      "Decoy protein name prefix. You can explicitly provide an empty string" +
          " as a parameter, in which case decoys won't be left out from the analysis.")
  protected List<String> decoyProtPrefix = Arrays.asList("rev_");
  @Parameter(names = {"--filter"}, description =
      "Path to a comma or tab delimited text file containing identifiers " +
          "of peptides to be kept in the analysis. See '-fc' option, by default the 1st column of the file is used.",
      validateValueWith = ExistingPathValidator.class, converter = PathParameter.class)
  protected Path filterFile;
  @Parameter(names = {"-fc", "--filterCol"}, description =
      "The column number in the tab/comma delimited file given " +
          "in '--filter' option.")
  protected int filterCol = 0;
  @Parameter(names = {"-u",
      "--use"}, description = "Only consider forwards, decoys or both in the analysis.")
  protected DecoyTreatment decoysTreatment = DecoyTreatment.FORWARDS_ONLY;
  @Parameter(names = {"-c", "--cache"}, arity = 1,
      description = "Whether to use caching - write parsed info from pepxml files to separate files (to turn off use '-c false').")
  protected boolean useCache = true;
  @Parameter(names = {"-x", "--delete_cache"}, description =
      "Will delete all cached files, you still need to provide proper path in '--in'." +
          " Won't do any KDE stuff, just the deletions.")
  protected boolean deleteCache = false;
  @Parameter(names = {"--flush"}, description = "Clear stored parameters from previous runs.")
  protected boolean flush = false;
  @Parameter(names = {
      "--cache_ext"}, description = "Extension for the cache file, which is used to avoid re-parsing pep.xml files")
  protected String cacheExt = ".kde-cache";
  @Parameter(names = {"-b"}, description =
      "Set bandwidths to be used for generating KDEs manually. " +
          "You can specify multiple comma separated values (e.g. -b 0.1,0.001). See '-bd' switch.")
  protected List<Double> bandwidths = new ArrayList<>();
  @Parameter(names = {"-ba", "--b_auto"}, arity = 1,
      description = "Will automatically estimate the bandwidth for each nominal mass.")
  protected boolean dynamicBandwidthEstimation = true;
  @Parameter(names = {"-bm", "--b_mz"}, hidden = true,
      description = "The m/z around which samples will be collected to evaluate the bandwidth. Will only work if non-null.")
  protected double autoBandwithTarget = Double.NaN;
  @Parameter(names = {"-bw", "--b_mz_wnd"}, hidden = true,
      description = "The m/z window used for -bm and -bd.")
  protected double autoBandwidthWindow = 0.15;
  @Parameter(names = {"-k",
      "--kernel"}, description = "What kernel will be used for KDE. Better not to change from default.")
  protected KDEKernelType kernelType = KDEKernelType.GAUSS_FAST;
  @Parameter(names = {"-s",
      "--score"}, description = "Parameter cutoffs, e.g. \"expect<=0.001\" or \"peptideprophet>0.95\"",
      validateWith = ScorePredicateFactoryParameter.class, converter = ScorePredicateFactoryParameter.class)
  protected List<ScorePredicate.Factory> scorePredicateFactories;
  @Parameter(names = {"-ml", "--mz_lo"}, description =
      "The lower m/z bound for which to do the KDE, can be left null, in which case" +
          " the whole range of mass differences from pep.xml files will be used. That filter is applied after reading pepxml or cache files,"
          +
          " so it's safe to reuse cache files with this option set to a different value.")
  protected Double mzLo = null;
  @Parameter(names = {"-mh", "--mz_hi"}, description = "See '--mz_lo' description")
  protected Double mzHi = null;
  @Parameter(names = {"-ms",
      "--mz_step"}, description = "The step used for m/z axis plotting of KDE.")
  protected double mzStep = 0.001;
  @Parameter(names = {"-mc", "--mass_correction"}, description =
      "Mass shift correction. ZERO_PEAK will use the shift of the" +
          " highest peak near delta mass of 0.0 to correct all other mass differences.")
  protected MassCorrection massCorrection = MassCorrection.NONE;
  @Parameter(names = {"--mc_paths"}, description =
      "Directories where to search for mass correction files. " +
          "Separate entries with commas.",
      validateWith = PathListParameter.class, converter = PathParameter.class, splitter = CommaParameterSplitter.class)
  protected List<Path> massCorFilePaths = new ArrayList<>();
  @Parameter(names = {"-f", "--force"}, description =
      "Forces overwriting files. If not set and some path needs to be created," +
          " but already exists - the program will exit.")
  protected boolean force = false;
  @Parameter(names = {"-o", "--out"},
      description = "If KDE peak detection is enabled (-p switch), the output will be written to this file.",
      validateWith = WriteableDirectoryParameter.class,
      converter = WriteableDirectoryParameter.class,
      validateValueWith = WriteableDirectoryParameter.class)
  protected Path outFilePath;
  @Parameter(names = {"-g",
      "--gui"}, description = "Run GUI version, where you can select all the parameters.")
  protected boolean runGui = false;
  @Parameter(names = {"--plot"}, arity = 1,
      description = "Should the KDE be plotted in GUI? (Use '--plot false' to turn off)")
  protected boolean plotKde = true;
  @Parameter(names = {"--plotder"}, hidden = true,
      description = "Plot second derivative of KDE?")
  protected boolean plotDer2 = false;
  @Parameter(names = {"--plotgmm"},
      description = "Plot the final GMM model? Very slow when zoomed in")
  protected boolean plotGmm = true;
  @Parameter(names = {"-p", "--peaks"}, arity = 1,
      description = "Should peak detection be run? (use '-p false' to turn off)")
  protected boolean detectPeaks = true;
  @Parameter(names = {"--clear"}, arity = 2, description =
      "Remove data points that are within delta mass ranges. " +
          "You can specify multiple ranges in pairs, e.g. \"--clear -0.5 0.5 2.5 3.5\"")
  protected List<Double> clearRange;
  @Parameter(names = {"-w", "--weights"},
      description = "Name of a score (from pepxml) to be used as weighting factor for KDE. If not specified all weights are 1.")
  protected String weightsScoreName;
  @Parameter(names = {"--minsupport"},
      description = "The minimum number of data points supporting a peak for it to be reported.")
  protected int gmmMinSupport = 20;
  @Parameter(names = {"--cutoff"},
      description = "For each nominal delta mass region only report peaks that are at least this % of the top peak.")
  protected double cutoff = 0.025;
  @Parameter(names = {"-omf", "--out_mz_fmt"},
      validateWith = DecimalFormatParameter.class,
      converter = DecimalFormatParameter.class,
      description = "Format to be used in the output for m/z values. See " +
          "https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html for details.")
  protected DecimalFormatWithToString outMzFmt = new DecimalFormatWithToString("0.#####");
  @Parameter(names = {"-oaf", "--out_ab_fmt"},
      validateWith = DecimalFormatParameter.class,
      converter = DecimalFormatParameter.class,
      description = "Format for the intensity/abundance in the output file, if peaks are detected. See '-omf'.")
  protected DecimalFormatWithToString outIntensityFmt = new DecimalFormatWithToString("0.#E0");
  @Parameter(names = {
      "--threads"}, description = "The max number of threads to be used. Zero means use all cores.")
  protected int threads = 0;
  @Parameter(names = {"--denoise"}, hidden = true,
      description =
          "Apply some denoising before peak detection. TOTAL_VARIATION takes 1 parameter, " +
              "FUSED_LASSO takes 2 parameters (use --denoiseParams to set them).")
  protected Denoising denoising = Denoising.FUSED_LASSO;
  @Parameter(names = {"--denoisingParams"}, hidden = true,
      description = "Parameters for the denoising algorithm (see --denoise).")
  protected List<Double> denoisingParams = new ArrayList<>();
  @Parameter(names = {"--denoisingPlot"}, hidden = true,
      description = "Apply some denoising before peak detection.")
  protected boolean denoisingPlot = false;
  @Parameter(names = {"--bin"}, hidden = true,
      description =
          "If your input is in the form of a tab delimited file with numbers (first column - value, optional"
              +
              " second column - weights), set this flag.")
  protected boolean isBinaryInput = false;
  @Parameter(names = {"--binDelimiter"}, hidden = true,
      description = "Works with --bin only. Each line of text input will be split according to this regular expression.")
  protected String delimiterRegEx = "\\s";
  @Parameter(names = {"--binHeader"}, hidden = true,
      description = "Works with --bin only. When parsing a text (delimited) file, if set to true will skip the first row.")
  protected boolean headerRow = false;
  @Parameter(names = {"--binColData"}, hidden = true,
      description = "Works with --bin only. If parsing a delimited file, this column will be used as the data input.")
  protected Integer colNumData = 0;
  @Parameter(names = {"--binColWeight"}, hidden = true,
      description = "Works with --bin only. If parsing a delimited file, this column can optionally be used as weights.")
  protected Integer colNumWeight = null;
  @Parameter(names = {"--gmmNum"}, hidden = true,
      description = "How many entries to distribute according to KDE distribution for GMM modeling.")
  protected int gmmNumEntries = 10000;
  @Parameter(names = {"--gmmMin"}, hidden = true,
      description = "Minimum number of entries to be accepted for GMM modeling.")
  protected int gmmMinEntries = 1;

//    @Parameter(names = {"-orf", "--out_rt_fmt"}, description = "printf() style format to be used in the output for RT values.")
//    protected String outRtFmt = "%.2f";
//
//    @Parameter(names = {"-oif", "--out_int_fmt"}, description = "printf() style format to be used in the output for intensity values.")
//    protected String outIntFmt = "%.1f";
  @Parameter(names = {"--kdeminpts"}, hidden = true,
      description =
          "KDE cutoff threshold. This number of points will be placed at the same exact spot and KDE "
              +
              "response evaluated. Any KDE values below that value are discarded.")
  protected int kdeMinPts = 10;

  public static OpenSearchParams parseParams(String[] args, Appendable out)
      throws ParameterException {
    log.debug("Program {} started", APP_NAME);
    OpenSearchParams params = new OpenSearchParams();
    JCommander jcom = new JCommander(params);
    try {
      jcom.setProgramName(APP_NAME);
      if (args.length == 0) {
        jcom.usage();
        return null;
      }
      jcom.parse(args);
      if (params.isHelp()) {
        jcom.usage();
        return null;
      }

      params.validate();

    } catch (ParameterException pe) {
      log.error(pe.getMessage());
      return null;
    }
    return params;
  }

  public boolean isHelp() {
    return help != null && help;
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(Integer threads) {
    this.threads = threads;
  }

  /**
   * The predicate for filtering search hits based on their mass difference.
   *
   * @return null if there are no bounds for mass diffs
   */
  public MassDiffRangePredicate getPredicateMassRange() {
    if (mzLo == null && mzHi == null && clearRange == null) {
      return null;
    }
    double mLo = mzLo == null ? Double.NEGATIVE_INFINITY : mzLo;
    double mHi = mzHi == null ? Double.POSITIVE_INFINITY : mzHi;
    double[][] clearRanges = null;
    if (clearRange != null) {
      int numExclusions = clearRange.size() / 2;
      clearRanges = new double[numExclusions][2];
      for (int i = 0; i < numExclusions; i++) {
        int lo = i * 2;
        int hi = lo + 1;
        clearRanges[i][0] = clearRange.get(lo);
        clearRanges[i][1] = clearRange.get(hi);
      }
    }
    return new MassDiffRangePredicate(mLo, mHi, clearRanges);
  }

  public List<Path> getMassCorFilePaths() {
    return massCorFilePaths;
  }

  public int getKdeMinPts() {
    return kdeMinPts;
  }

  public int getGmmMinSupport() {
    return gmmMinSupport;
  }

  public int getGmmNumEntries() {
    return gmmNumEntries;
  }

  public void setGmmNumEntries(int gmmNumEntries) {
    this.gmmNumEntries = gmmNumEntries;
  }

  public int getGmmMinEntries() {
    return gmmMinEntries;
  }

  public void setGmmMinEntries(int gmmMinEntries) {
    this.gmmMinEntries = gmmMinEntries;
  }

  public List<Path> getInFilePath() {
    return inFilePath;
  }

  public void setInFilePath(List<Path> inFilePath) {
    this.inFilePath = inFilePath;
  }

  public String getInFileRegex() {
    return inFileRegex;
  }

  public void setInFileRegex(String inFileRegex) {
    this.inFileRegex = inFileRegex;
  }

  public boolean isFollowSymlinks() {
    return followSymlinks;
  }

  public void setFollowSymlinks(boolean followSymlinks) {
    this.followSymlinks = followSymlinks;
  }

  public boolean isBinaryInput() {
    return isBinaryInput;
  }

  public String getDelimiterRegEx() {
    return delimiterRegEx;
  }

  public boolean isHeaderRow() {
    return headerRow;
  }

  public Integer getColNumData() {
    return colNumData;
  }

  public Integer getColNumWeight() {
    return colNumWeight;
  }

  public LinkOption[] getFollowSymlinkOptions() {
    return followSymlinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
  }

  public Boolean getHelp() {
    return help;
  }

  public void setHelp(Boolean help) {
    this.help = help;
  }

  public List<String> getDecoyProtPrefix() {
    return decoyProtPrefix;
  }

  public void setDecoyProtPrefix(List<String> decoyProtPrefix) {
    this.decoyProtPrefix = decoyProtPrefix;
  }

  public DecoyDetector getDecoyDetector() {
    List<String> decoyProtPrefixes = decoyProtPrefix;
    DecoyDetector decoyDetector;
    if (decoyProtPrefixes == null || decoyProtPrefixes.isEmpty()) {
      decoyDetector = new DecoyDetectorAcceptAll();
    } else {
      decoyDetector = new DecoyDetectorByProtName(decoyProtPrefixes);
    }
    return decoyDetector;
  }

  public DecimalFormat getOutIntensityFmt() {
    return outIntensityFmt;
  }

  public void setOutIntensityFmt(DecimalFormatWithToString outIntensityFmt) {
    this.outIntensityFmt = outIntensityFmt;
  }

  public boolean isDenoisingPlot() {
    return denoisingPlot;
  }

  public void setDenoisingPlot(boolean denoisingPlot) {
    this.denoisingPlot = denoisingPlot;
  }

  public Path getFilterFile() {
    return filterFile;
  }

  public int getFilterCol() {
    return filterCol;
  }

  public List<ScorePredicate.Factory> getScorePredicateFactories() {
    return scorePredicateFactories;
  }

  public void setScorePredicateFactories(List<ScorePredicate.Factory> scorePredicateFactories) {
    this.scorePredicateFactories = scorePredicateFactories;
  }

  public boolean isUseCache() {
    return useCache;
  }

  public void setUseCache(boolean useCache) {
    this.useCache = useCache;
  }

  public boolean isDeleteCache() {
    return deleteCache;
  }

  public void setDeleteCache(boolean deleteCache) {
    this.deleteCache = deleteCache;
  }

  public String getCacheExt() {
    return cacheExt;
  }

  public void setCacheExt(String cacheExt) {
    this.cacheExt = cacheExt;
  }

  public List<Double> getBandwidths() {
    return bandwidths;
  }

  public void setBandwidths(List<Double> bandwidths) {
    this.bandwidths = bandwidths;
  }

  public boolean isDynamicBandwidthEstimation() {
    return dynamicBandwidthEstimation;
  }

  public void setDynamicBandwidthEstimation(boolean dynamicBandwidthEstimation) {
    this.dynamicBandwidthEstimation = dynamicBandwidthEstimation;
  }

  public List<NamedBandwidth> getNamedBandwidths() {
    List<NamedBandwidth> result = new ArrayList<>();
    if (dynamicBandwidthEstimation) {
      result.add(new NamedBandwidth("Auto at each nominal mass", Double.NaN, Double.NaN,
          autoBandwidthWindow));
    }
    if (!Double.isNaN(autoBandwithTarget)) {
      String name = String.format("Auto@%.2f[%.2f]", autoBandwithTarget, autoBandwidthWindow);
      NamedBandwidth bandwidth = new NamedBandwidth(name, Double.NaN, autoBandwithTarget,
          autoBandwidthWindow);
      result.add(bandwidth);
    }
    for (Double b : bandwidths) {
      DecimalFormat fmt = new DecimalFormat("0.##E0");
      String name = fmt.format(b);
      result.add(new NamedBandwidth(name, b));
    }
    return result;
  }

  public List<Double> getDenoisingParams() {
    return denoisingParams;
  }

  public void setDenoisingParams(List<Double> denoisingParams) {
    this.denoisingParams = denoisingParams;
  }

  public Double getAutoBandwithTarget() {
    return autoBandwithTarget;
  }

  public void setAutoBandwithTarget(Double autoBandwithTarget) {
    this.autoBandwithTarget = autoBandwithTarget;
  }

  public Double getAutoBandwidthWindow() {
    return autoBandwidthWindow;
  }

  public void setAutoBandwidthWindow(Double autoBandwidthWindow) {
    this.autoBandwidthWindow = autoBandwidthWindow;
  }

  public DecoyTreatment getDecoysTreatment() {
    return decoysTreatment;
  }

  public void setDecoysTreatment(DecoyTreatment decoysTreatment) {
    this.decoysTreatment = decoysTreatment;
  }

  public KDEKernelType getKernelType() {
    return kernelType;
  }

  public void setKernelType(KDEKernelType kernelType) {
    this.kernelType = kernelType;
  }

  public String getWeightsScoreName() {
    return weightsScoreName;
  }

  public void setWeightsScoreName(String weightsScoreName) {
    this.weightsScoreName = weightsScoreName;
  }

  public boolean isUseWeights() {
    return weightsScoreName != null && !weightsScoreName.isEmpty();
  }

  public WeightFetcherSearchHitDiff.Factory getWeightFetcherFactory() {
    if (!isUseWeights()) {
      return null;
    }
    return new WeightFetcherSearchHitDiff.Factory(weightsScoreName);
  }

  public Double getMzLo() {
    return mzLo;
  }

  public void setMzLo(Double mzLo) {
    this.mzLo = mzLo;
  }

  public Double getMzHi() {
    return mzHi;
  }

  public void setMzHi(Double mzHi) {
    this.mzHi = mzHi;
  }

  public double getMzStep() {
    return mzStep;
  }

  public void setMzStep(double mzStep) {
    this.mzStep = mzStep;
  }

  public DecimalFormat getOutMzFmt() {
    return outMzFmt;
  }

  public void setOutMzFmt(DecimalFormatWithToString outMzFmt) {
    this.outMzFmt = outMzFmt;
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public Path getOutFilePath() {
    return outFilePath;
  }

  public void setOutFilePath(Path outFilePath) {
    this.outFilePath = outFilePath;
  }

  public boolean isPlotKde() {
    return plotKde;
  }

  public void setPlotKde(boolean plotKde) {
    this.plotKde = plotKde;
  }

  public boolean isPlotDer2() {
    return plotDer2;
  }

  public void setPlotDer2(boolean plotDer2) {
    this.plotDer2 = plotDer2;
  }

  public boolean isDetectPeaks() {
    return detectPeaks;
  }

  public void setDetectPeaks(boolean detectPeaks) {
    this.detectPeaks = detectPeaks;
  }

  public MassCorrection getMassCorrection() {
    return massCorrection;
  }

  public void setMassCorrection(MassCorrection massCorrection) {
    this.massCorrection = massCorrection;
  }

  public Denoising getDenoising() {
    return denoising;
  }

  public void setDenoising(Denoising denoising) {
    this.denoising = denoising;
  }

  /**
   * Only run this method after the params have been initialised with actual program input.
   */
  public void validate() throws ParameterException {
    if (deleteCache) // this should be allowed no matter what
    {
      return;
    }

    if (cacheExt == null || cacheExt.isEmpty()) {
      throw new ParameterException(
          "Cache file extension ('--cache_ext' option) can not be left empty.");
    }

    if (bandwidths.isEmpty() && !dynamicBandwidthEstimation) {
      throw new ParameterException(
          "You must provide at least one bandwidth using '-b' option or use dynamic " +
              "bandwidth estimation ('-ba' option) or provide a specific m/z to be used " +
              "for bandwith estimation ('-bm' option). You can also combine those.");
    }

    if (dynamicBandwidthEstimation && Double.isNaN(autoBandwidthWindow)) {
      throw new ParameterException("When using '-hd' option you must provide '-hw' as well.");
    }

    if (!getNamedBandwidths().isEmpty() && detectPeaks && outFilePath != null) {
      // in case of peak detection we check if the file paths are available
      if (Files.exists(outFilePath)) {
        if (!Files.isDirectory(outFilePath)) {
          throw new ParameterException("The provided output path exists, but it"
              + "s not a directory. This is not allowed.");
        }
      } else {
        // Output path doesn't exist. Try creating it
        try {
          Files.createDirectories(outFilePath);
        } catch (IOException ex) {
          throw new ParameterException("Could not create directory structure for the output.", ex);
        }
      }
      if (!Files.isWritable(outFilePath)) {
        throw new ParameterException(String
            .format("Output path (%s) does not appear to be writeable. Exiting.", outFilePath));
      }

      if (force) {
        return;
      }
      for (NamedBandwidth nb : getNamedBandwidths()) {
        Path nbOutputPath = nb.createOutputPath(outFilePath);
        if (Files.exists(nbOutputPath)) {
          throw new ParameterException(
              String.format("You've requested to store the output of peak detection, \n" +
                  "and did not specify --force option. \n" +
                  "One of the output files to be generated already exists, \n" +
                  "won't overwrite: \n" +
                  "(%s).", nbOutputPath));
        }
      }
    }
  }

  public void setPlotGmm(boolean plotGmm) {
    this.plotGmm = plotGmm;
  }

  public void setClearRange(List<Double> range) {
    if (range.size() != 2) {
      throw new IllegalArgumentException("Clear List Range size must be excatly 2.");
    }
    this.clearRange = new ArrayList<>(range.size());
    this.clearRange.addAll(range);
  }

  public boolean isRunGui() {
    return runGui;
  }

  public void setRunGui(boolean runGui) {
    this.runGui = runGui;
  }
}
