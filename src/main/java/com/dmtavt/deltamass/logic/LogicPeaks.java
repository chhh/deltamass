package com.dmtavt.deltamass.logic;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.beust.jcommander.ParameterException;
import com.dmtavt.deltamass.data.PepSearchFile;
import com.dmtavt.deltamass.logic.LogicKde.ProcessingSegment;
import com.dmtavt.deltamass.utils.PeakUtils;
import com.dmtavt.deltamass.utils.PeakUtils.Peak;
import com.dmtavt.deltamass.utils.PeakUtils.PeakDetectionConfig;
import com.github.chhh.utils.MathUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ptm.PtmFactory;
import umich.ptm.exceptions.ModParsingException;
import umich.ptm.mod.Mod;
import umich.ptm.mod.Mods;

public class LogicPeaks {

  private static final Logger log = LoggerFactory.getLogger(LogicPeaks.class);
  public static final String OUTPUT_FILE_NAME = "deltamass-peaks.csv";
  final CommandPeaks cmd;

  public LogicPeaks(CommandPeaks cmd) {
    this.cmd = cmd;
  }

  public void run() throws ParameterException {
    validateInputs();

    LogicInputFiles inputFiles = new LogicInputFiles(cmd.optsInputFiles);
    List<PepSearchFile> searchFiles = inputFiles.run();
    LogicKde logicKde = new LogicKde(cmd.optsKde, searchFiles);
    List<ProcessingSegment> processingSegments = logicKde.run();

    // detect peaks in segments
    final PeakDetectionConfig conf = new PeakDetectionConfig(cmd.optsPeaks.minPeakPct,
        cmd.optsPeaks.minPsmsPerGmm);
    final List<Detected> detecteds = new ArrayList<>();
    log.debug("Running peak detection");
    for (ProcessingSegment segment : processingSegments) {
      detecteds.add(LogicPeaks.detectPeaks(segment, conf));
    }

    // write peaks
    final Path outPath = cmd.optsPeaks.out;
    if (outPath == null) {
      // if no path, just print to screen
      writePeaks(System.out, detecteds, cmd.optsPeaks.digitsMz, cmd.optsPeaks.digitsAb);
    } else {
      try {
        // we asked for user permission to overwrite the file before running
        final Path outputFilePath = LogicPeaks.getOutputFilePath(outPath);
        LogicPeaks.writePeaks(outputFilePath, detecteds, cmd.optsPeaks.digitsMz, cmd.optsPeaks.digitsAb);
      } catch (IOException e) {
        log.error("Error writing output file: " + outPath.toString(), e);
      }
    }
  }

  private void validateInputs() {
    final Path outPath = cmd.optsPeaks.out;
    if (outPath != null) {
      // check if output file exists
      final Path outFilePath = getOutputFilePath(outPath);
      if (!cmd.optsPeaks.overwrite && Files.exists(outFilePath)
          && Files.isRegularFile(outFilePath)) {
        String msg = "Output file exists:\n"
            + "  " + outFilePath.toString() + "\n"
            + "Overwrite option (\"-w\" or \"--force\") not specified. Not proceeding.";
        throw new ParameterException(msg);
      }
    }
  }

  public static String getDefaultOutputFileName() {
    return OUTPUT_FILE_NAME;
  }

  /**
   * If the given path not exists or is a regular file, just returns the same path.
   * Otherwise (i.e. if given an existing directory) returns a file with default name in that
   * directory.
   *
   * User should be asked about overwriting if the path returned by this method exists.
   */
  public static Path getOutputFilePath(Path userSpecifiedPath) {
    if (!Files.exists(userSpecifiedPath)) {
      return userSpecifiedPath;
    }
    // provided path exists, but is a directory
    if (Files.isDirectory(userSpecifiedPath)) {
      return userSpecifiedPath.resolve(getDefaultOutputFileName());
    }
    // existing regular file, should ask about overwriting
    return userSpecifiedPath;
  }

  /**
   * Will overwrite any existing file, it's up to the user to figure if the file exists.
   * Will also create all the missing directories along the way to the file.
   *
   * @param path Should point to the file to write to.
   */
  public static void writePeaks(Path path, List<Detected> detecteds,
      Integer decimalDigitsMz, Integer decimalDigitsAb) throws IOException {
    if (!Files.exists(path.getParent())) {
      Files.createDirectories(path.getParent());
    }
    try (OutputStream os = Files.newOutputStream(path, CREATE, TRUNCATE_EXISTING)) {
      PrintStream ps = new PrintStream(os, true, "UTF-8");
      log.info("Writing peaks to file: " + path.toString());
      writePeaks(ps, detecteds, decimalDigitsMz, decimalDigitsAb);
    }
  }

  /**
   * @param decimalDigitsMz The format for printing masses. Can be null, default will be used.
   * @param decimalDigitsAb The format for printing intensities. Can be null, default will be used.
   */
  public static void writePeaks(PrintStream ps, List<Detected> detecteds,
      Integer decimalDigitsMz, Integer decimalDigitsAb) {
    ArrayList<GmmForPrinting> toPrint = detecteds.stream()
        .flatMap(detected -> detected.gmms.stream().map(GmmForPrinting::new))
        .sorted((o1, o2) -> Double.compare(o2.score, o1.score))
        .collect(Collectors.toCollection(ArrayList::new));

    final PeaksWriter pw = PeaksWriter.getDefault(decimalDigitsMz, decimalDigitsAb);
    final String delimiterCol = ", ";
    final String delimiterRow = "\n";
    String rows = Stream
        .concat(Stream.of(pw.colsNames), toPrint.stream().map(pw.mapper))
        .map(cols -> String.join(delimiterCol, cols))
        .collect(Collectors.joining(delimiterRow));

    ps.println(rows);
  }

  private static class ModEntry {
    final double m;
    final double mError;
    final double likelihood;
    final Mod mod;

    public ModEntry(double m, double mError, double likelihood, Mod mod) {
      this.m = m;
      this.mError = mError;
      this.likelihood = likelihood;
      this.mod = mod;
    }
  }

  public static class PeaksWriter {

    public final List<String> colsNames;
    public final Function<GmmForPrinting, List<String>> mapper;

    private final static String valSep = "::";
    private final static String entrySep = "; ";

    public PeaksWriter(List<String> colsNames, Function<GmmForPrinting, List<String>> mapper) {
      this.colsNames = colsNames;
      this.mapper = mapper;
    }

    private static final String fmt(String format, Object param) {
      return String.format(format, param);
    }

    public static PeaksWriter getDefault(Integer decimalDigitsMz, Integer decimalDigitsAb) {

      decimalDigitsMz = decimalDigitsMz != null ? decimalDigitsMz : 4;
      decimalDigitsAb = decimalDigitsAb != null ? decimalDigitsAb : 1;

      // PTM annotations
      final Mods mods;
      try {
        mods = PtmFactory.getMods(EnumSet.of(PtmFactory.SOURCE.GEN, PtmFactory.SOURCE.UNIMOD));
      } catch (ModParsingException e) {
        throw new IllegalStateException("Could not create a list of annotation PTMs");
      }

      final String fMz = "%." + decimalDigitsMz + "f";
      final String fAb = "%." + decimalDigitsAb + "f";
      final String f1 = "%.1f";
      final String f2 = "%.2f";
      final String f3 = "%.3f";
      final String f4 = "%.4f";
      final DecimalFormat df1 = new DecimalFormat("0.0E0");

      final Function<GmmForPrinting, String> annotationProvider = gfp -> {
        final double m = gfp.gmmc.mu;
        final double sd = gfp.gmmc.sigma;
        final double sdDistance = 2.0; // annotations max 2 SD away from peaks in KDE

        return mods.findByMass(m - sdDistance * sd, m + sdDistance * sd).stream()
            .map(mod -> {
              final Double massMono = mod.getComposition().getMassMono();
              if (massMono == null) {
                log.warn(
                    "Monoisotopic mass of elemental composition was null while mapping possible mods for a peak");
                return null;
              }
              final double mError = Math.abs(massMono - m);
              final double p = MathUtils.normalPdf(mError,0, sd);
              final double pMax = MathUtils.normalPdf(0, 0, sd);
              final double pNorm = p / pMax;
              final double likelihood = Double.isFinite(pNorm) ? pNorm : 0;
              return new ModEntry(massMono, mError, likelihood, mod);
            })
            .filter(Objects::nonNull)
            .sorted((o1, o2) -> Double.compare(o2.likelihood, o1.likelihood))
            .map(me -> me.mod.getRef() + valSep
                + "m=" + fmt(f4, me.m) + "Da" + valSep
                + "mErr=" + df1.format(me.mError) + "Da" + valSep
                + "q=" + fmt(f2, me.likelihood) + valSep
                + me.mod.getDescShort())
            .collect(Collectors.joining(entrySep, "\"", "\""));
      };

      final List<String> names = Arrays
          .asList("dm", "fwhm", "stddev", "support", "intensity", "quality", "score", "annotations");

      final Function<GmmForPrinting, List<String>> mapper = gfp -> {
        List<String> cols = new ArrayList<>(names.size());
        cols.add(fmt(fMz, gfp.gmmc.mu));
        cols.add(fmt(fMz, MathUtils.fwhmFromSigma(gfp.gmmc.sigma)));
        cols.add(fmt(fMz, gfp.gmmc.sigma));
        cols.add(fmt(f1, gfp.gmmc.psmSupportApprox));
        cols.add(fmt(fAb, gfp.gmmc.kdeValAtPeakOrigin));
        cols.add(fmt(f2, gfp.quality));
        cols.add(fmt(f2, gfp.score));
        cols.add(annotationProvider.apply(gfp));
        return cols;
      };

      return new PeaksWriter(names, mapper);
    }
  }

  public static class GmmForPrinting {
    public final GmmComponent gmmc;
    public final double quality;
    public final double score;

    public GmmForPrinting(GmmComponent gmmc) {
      this.gmmc = gmmc;
      quality = gmmc.peakOrigin.intensity;
      score = quality * gmmc.psmSupportApprox;
    }
  }

  /**
   * Detect peaks in a processed data segment. Processed means KDE was calculated for it.
   * @throws IllegalStateException if {@link ProcessingSegment#isProcessed()} is false.
   */
  public static Detected detectPeaks(ProcessingSegment segment, PeakDetectionConfig conf) {
    if (!segment.isProcessed()) {
      throw new IllegalStateException("Unprocessed segment passed to detectPeaks()");
    }

    // invert sign of 2nd derivative
    double[] der2Inv = Arrays.copyOf(segment.der2, segment.der2.length);
    for (int i = 0; i < der2Inv.length; i++) {
      der2Inv[i] *= -1;
    }

    final Detected detected = new Detected(segment);
    // local minima in the 2nd derivative
    List<PeakUtils.PeakApprox> der2Peaks = PeakUtils
        .peakLocations(der2Inv, 0, der2Inv.length, peakApprox -> true);
    // remove flanking plateaus
    der2Peaks.removeIf(p -> p.valTop == 0.0);

    // fit peaks with parabolas
    for (PeakUtils.PeakApprox peakApprox : der2Peaks) {
      Peak peak = PeakUtils.fitPeakByParabola(peakApprox, segment.x, der2Inv, true);
      detected.peaks.add(peak);
    }

    // fit GMM
    List<GmmComponent> gmms = gmm(detected, conf);
    detected.gmms = gmms;

    return detected;
  }

  private static List<GmmComponent> gmm(Detected detected, PeakDetectionConfig conf) {
    final int len = detected.peaks.size();
    final double[] weights = new double[len];
    final double[] mus = new double[len];
    final double[] sigmas = new double[len];
    double sumWeights = 0;
    for (int i = 0; i < len; i++) {
      Peak p = detected.peaks.get(i);
      weights[i] = p.intensity;
      sumWeights += weights[i];
      mus[i] = p.location;
      switch (p.widthType) {
        case AT_BASE:
          sigmas[i] = p.width / 6.0;
          break;
        case FWHM:
          sigmas[i] = p.width / 2.0;
          break;
        default:
          throw new AssertionError("Unknown enum value");
      }
    }
    for (int i = 0; i < len; i++) {
      weights[i] /= sumWeights;
    }

    List<GmmComponent> gmms = new ArrayList<>();
    for (int i = 0; i < len; i++) {
      Peak peak = detected.peaks.get(i);
      ProcessingSegment seg = detected.processed;
      final int psmsInSeg = seg.data.spmInfos.size();
      final double psmsInKdeAuc = psmsInSeg * seg.kdeAuc;

      // KDE value at peak's location interpolated from the calculated KDE grid
      LinearInterpolator interpolator = new LinearInterpolator();
      PolynomialSplineFunction iKde = interpolator.interpolate(seg.x, seg.kde);
      final double kdeValAtPeakOrigin = iKde.value(peak.location) * psmsInKdeAuc;

      final double psmSupport = psmsInKdeAuc * weights[i];
      GmmComponent gmmc = new GmmComponent(peak, kdeValAtPeakOrigin, weights[i], mus[i], sigmas[i], psmSupport);
      if (gmmc.psmSupportApprox < conf.minPsmsPerGmm) {
        continue;
      }
      gmms.add(gmmc);
    }

    return gmms;

    // TODO: optimization search here, not urgent

//    Normal.pdf()
//    BacktrackingArmijoLineSearch lineSearch = new BacktrackingArmijoLineSearch();
//
//    LBFGS lbfgs = new LBFGS(5, 100, lineSearch);
//    Vec solution = new DenseVector(sz * 2);
//
//    Function f = new Function() {
//      @Override
//      public double f(double... x) {
//        return 0;
//      }
//
//      @Override
//      public double f(Vec x) {
//        return 0;
//      }
//    };
//
//
//    lbfgs.optimize(1e-12, solution, new DenseVector(Arrays.asList(0.0)), func, funcp, null);
//    double mCorrection = solution.get(0);
  }

  public static class Detected {
    public final ProcessingSegment processed;
    public List<Peak> peaks;
    public List<GmmComponent> gmms;


    public Detected(ProcessingSegment processed) {
      this.processed = processed;
      peaks = new ArrayList<>();
      gmms = new ArrayList<>();
    }
  }

  public static class GmmComponent {
    final Peak peakOrigin;
    final double kdeValAtPeakOrigin;
    final double weight;
    final double mu;
    final double sigma;
    final double psmSupportApprox;

    public GmmComponent(Peak associatedPeak, double kdeValAtPeakOrigin, double weight,
        double mu, double sigma, double psmSupportApprox) {
      this.peakOrigin = associatedPeak;
      this.kdeValAtPeakOrigin = kdeValAtPeakOrigin;
      this.weight = weight;
      this.mu = mu;
      this.sigma = sigma;
      this.psmSupportApprox = psmSupportApprox;
    }
  }
}
