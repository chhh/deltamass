package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.data.PepSearchResult;
import com.dmtavt.deltamass.data.Spm;
import com.dmtavt.deltamass.parsers.ILcmsParser;
import com.dmtavt.deltamass.parsers.IParserFactory;
import com.dmtavt.deltamass.parsers.LcmsParserRegistry;
import com.github.chhh.utils.exceptions.ParsingException;
import com.github.chhh.utils.ser.DataStoreUtils;
import edu.umich.andykong.msutils.mscalibrator.QuantPair;
import edu.umich.andykong.msutils.mscalibrator.SpectralIndex;
import edu.umich.andykong.msutils.mscalibrator.Spectrum;
import edu.umich.andykong.msutils.mscalibrator.SpectrumResult;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ms.datatypes.LCMSData;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.index.Index;
import umich.ms.datatypes.index.IndexElement;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scancollection.IScanCollection;
import umich.ms.datatypes.scancollection.ScanIndex;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.util.IntervalST;
import umich.ptm.chem.Table;

public class MassCorrectionMs1 implements MassCorrector {
  private static final Logger log = LoggerFactory.getLogger(MassCorrectionMs1.class);
  private static final double MASS_H = 1.0078250321;
  private static final double MASS_C_DIFF = 1.0033548378;

  /** This score name is used for FDR calculation (to sort the PSM entries). */
  private final String scoreName;
  /** Where LCMS files will be searched. */
  private final List<Path> additionalSearchPaths;
  private final boolean noCache;

  /**
   *
   * @param scoreName For FDR estimation.
   * @param additionalSearchPaths Where LCMS files will be searched.
   */
  public MassCorrectionMs1(String scoreName, List<Path> additionalSearchPaths, boolean noCache) {
    this.scoreName = scoreName;
    this.additionalSearchPaths = additionalSearchPaths != null ? additionalSearchPaths :
        Collections.emptyList();
    this.noCache = noCache;
  }

  private String substrToLastDot(String text) {
    final int idx = text.lastIndexOf('.');
    return idx < 0 ? text : text.substring(0, idx);
  }

  /**
   * @return NULL if the given scan was empty, i.e. had no data values.
   * @throws FileParsingException
   */
  private static Spectrum convert(IScan scan, Index<?> idx, String baseName) throws FileParsingException {
    IndexElement idxElem = idx.getByNum(scan.getNum());
    int scanNumRaw = idxElem.getRawNumber();
    ISpectrum spectrum = scan.fetchSpectrum();
    if (spectrum == null || spectrum.getMZs().length == 0) {
      return null;
    }

    Spectrum ns = new Spectrum(spectrum.getMZs().length);
    try {
      ns.charge = (byte) scan.getPrecursor().getCharge().intValue();
    } catch (Exception e) {
      ns.charge = 0;
    }
    try {
      ns.precursorMass = scan.getPrecursor().getMzTarget();
    } catch (Exception e) {
      ns.precursorMass = 0;
    }
    ns.scanNum = scanNumRaw;
    ns.msLevel = (byte) scan.getMsLevel().intValue();
    ns.retentionTime = scan.getRt();
    ns.scanName = baseName + "." + scanNumRaw + "." + scanNumRaw + "." + ns.charge;

    for (int i = 0; i < ns.peakMZ.length; i++) {
      ns.peakMZ[i] = (float) spectrum.getMZs()[i];
      ns.peakInt[i] = (float) spectrum.getIntensities()[i];
    }

    return ns;
  }

  private static SpectralIndex buildSpectralIndex(LCMSData data, String baseName) throws FileParsingException {
    final SpectralIndex si = new SpectralIndex();
    IScanCollection scans = data.getScans();

    IntervalST<Double, TreeMap<Integer, IScan>> ms1ranges = scans.getMapMsLevel2rangeGroups().get(1);
    if (ms1ranges != null && ms1ranges.size() != 0 && ms1ranges.size() < 100) {
      throw new IllegalStateException("Likely a DIA run. Only standard DDA runs supported.");
    }
    ScanIndex index = scans.getMapMsLevel2index().get(1);
    if (index == null || index.getNum2scan() == null)
      throw new IllegalStateException("Index was not built");
    TreeMap<Integer, IScan> num2scan = index.getNum2scan();

    Index<?> sourceIndex = data.getSource().fetchIndex();
    if (sourceIndex == null)
      throw new IllegalStateException("Could not fetch index");

    for (Entry<Integer, IScan> kv : num2scan.entrySet()) {
      IScan scan = kv.getValue();
      Spectrum spec = convert(scan, sourceIndex, baseName);
      si.add(spec);
    }

    return si;
  }

  @Override
  public void apply(PepSearchResult pepSearchResult) throws IOException {
    if (!pepSearchResult.scoreMapping.containsKey(scoreName))
      throw new UnsupportedOperationException(String.format("Peptide search result '%s' didn't "
          + "contain score with name '%s', which is needed for FDR estimation.",
          pepSearchResult.sourceFileName, scoreName));
    final int scoreIndex = pepSearchResult.scoreMapping.get(scoreName);

    CalibrationData calibrationData = null;
    if (noCache) {
      calibrationData = createCalibration(pepSearchResult, scoreIndex);
    } else {
      Path cached = CacheLocator
          .locateForCal(Paths.get(pepSearchResult.sourceFileDir, pepSearchResult.sourceFileName),
              pepSearchResult.rawFileName);
      if (Files.exists(cached)) {
        try {
          log.info("Reading cached calibration: {}", cached);
          calibrationData = DataStoreUtils.deserialize(cached, CalibrationData.class);
        } catch (ParsingException e) {
          log.warn("Could not load cached cal file: {}", cached);
        }
      }
      if (calibrationData == null) {
        calibrationData = createCalibration(pepSearchResult, scoreIndex);
        try {
          DataStoreUtils.serialize(cached, calibrationData);
        } catch (ParsingException e1) {
          log.warn("Could not write calibration cache file: {}", cached);
        }
      }
    }

    applyCalibration(pepSearchResult.spms, calibrationData);
    pepSearchResult.massCorrection = MassCorrection.PEP_ID;
  }

  private CalibrationData createCalibration(PepSearchResult pepSearchResult, int scoreIndex)
      throws IOException {
    log.info("Building calibration for: {}/{}", pepSearchResult.sourceFileName, pepSearchResult.rawFileName);

    // search for LCMS file
    final Path pepidFilePath = Paths.get(pepSearchResult.sourceFileDir);
    final String origLcmsFn = pepSearchResult.rawFileName.toLowerCase();
    final String origLcmsFnNoExt = substrToLastDot(origLcmsFn);

    final ArrayList<Path> searchPaths = new ArrayList<>();
    searchPaths.add(pepidFilePath);
    searchPaths.addAll(additionalSearchPaths);

    Path lcmsPath = null;
    for (Path searchPath : searchPaths) {
      Optional<Path> match = Files.walk(searchPath)
          .filter(path -> {
            final String fn = path.getFileName().toString().toLowerCase();
            final String fnNoExt = substrToLastDot(fn);
            if (!fnNoExt.equals(origLcmsFnNoExt)) {
              return false;
            }
            return LcmsParserRegistry.find(path) != null;
          }).findFirst();
      if (match.isPresent()) {
        lcmsPath= match.get();
        break;
      }
    }
    if (lcmsPath == null) {
      String msg = String.format("Could not find LCMS file ('%s') for pep id file: %s",
          pepSearchResult.rawFileName, pepSearchResult.sourceFileName);
      log.error(msg);
      throw new IOException(msg);
    }
    IParserFactory<ILcmsParser> factory = LcmsParserRegistry.find(lcmsPath);
    if (factory == null)
      throw new IllegalStateException("Factory should not be null, we have checked for that already.");
    ILcmsParser lcmsParser = factory.create(lcmsPath);

    // FDR for search results
    SpecAssembly sa = new SpecAssembly(scoreIndex);
    sa.addPepSearchResult(pepSearchResult);
    sa.buildFdrByScore();

    // read LCMS
    log.info("Reading LCMS data: {}", lcmsPath);
    LCMSData lcmsData = lcmsParser.parse(LCMSDataSubset.MS1_WITH_SPECTRA);
    log.info("Building spectral index: {}", lcmsPath);
    final int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4));
    final String baseName = substrToLastDot(pepSearchResult.rawFileName);
    SpectralIndex si;
    try {
      si = buildSpectralIndex(lcmsData, baseName);
      si.build();
    } catch (Exception e) {
      throw new IOException(e);
    }

    // create calibration
    return calibrate(sa, si);
  }

  private static class SpecAssembly {
    Map<String, Spm> spmMap;
    List<Spm> spms;
    final int scoreIndex;
    boolean[] isDecoy;
    double[] fdr;
    int[] ppprob;
    int[] bestProb;

    public SpecAssembly(int scoreIndex) {
      this.scoreIndex = scoreIndex;
      this.spmMap = new HashMap<>();
    }

    public void addPepSearchResult(PepSearchResult psr) {
      for (Spm spm : psr.spms) {
        Spm old = spmMap.putIfAbsent(spm.spectrumId, spm);
        if (old != null)
          log.warn("While adding SearchResult encountered the same spectrumId multiple times.");
      }
    }

    private static double[] calculateFdr(int[] probs, boolean[] isDecoy) {
      int[][] counts = new int[10001][4];
      double[] fdrs = new double[10001];
      for (int i = 0; i < probs.length; i++) {
        counts[probs[i]][isDecoy[i] ? 1 : 0]++;
      }
      counts[10000][2] = counts[10000][0];
      counts[10000][3] = counts[10000][1];
      for (int i = 9999; i >= 0; i--) {
        counts[i][2] = counts[i + 1][2] + counts[i][0];
        counts[i][3] = counts[i + 1][3] + counts[i][1];
      }
      double bestFDR = 1;
      for (int i = 0; i <= 10000; i++) {
        double cFDR = ((double) counts[i][3]) / counts[i][2];
        if (cFDR < bestFDR) {
          bestFDR = cFDR;
        }
        fdrs[i] = bestFDR;
      }
      return fdrs;
    }

    void buildFdrByScore() {
      spms = new ArrayList<>(spmMap.values());
      spms.sort(Comparator.comparingDouble(o -> o.scores[scoreIndex]));
      int nDecoy = 0, nFwd = 0;

      for (Spm spm : spms) {
        if (spm.isDecoy) nDecoy++;
        else nFwd++;
      }

      fdr = new double[spms.size()];
      double bestFdr = 1;
      for (int i = spms.size() - 1; i >= 0; i--) {
        double curFdr = nDecoy / (double)nFwd;
        if (curFdr < bestFdr) {
          bestFdr = curFdr;
        }
        fdr[i] = bestFdr;
        if (spms.get(i).isDecoy) nDecoy--;
        else nFwd--;
      }
    }
  }

  public CalibrationData calibrate(SpecAssembly sa, SpectralIndex sis) {
    //perform MS1 traces
    ArrayList<QuantPair[]> ms1traces = new ArrayList<>();
    double[] adj_delta = new double[sa.spms.size()];
    HashMap<String, Double> adj_delta_map = new HashMap<>();
    double max_RT = 0, max_MZ = 0;

    for (int i = 0; i < sa.spms.size(); i++) {
      Spm spm = sa.spms.get(i);
      double calc_mass = spm.mCalcNeutral;
      double calc_mz = (calc_mass + spm.charge * MASS_H) / spm.charge;
      double ms1tol = 20;
      double rt = spm.rtSec / 60;
      QuantPair[] qp = new QuantPair[3];

//      for (int ccharge = 1; ccharge <= 5; ccharge++) {
//        for (int dc = -5; dc <= 5; dc++) {
//          QuantPair cqp = sis.quantXIC(calc_mz + (dc * MASS_C_DIFF/ ccharge), rt, ms1tol);
//        }
//      }

      for (int j = 0; j < 3; j++) {
        qp[j] = sis.quantXIC(calc_mz + (j * MASS_C_DIFF / spm.charge), rt, ms1tol);
      }
      if (qp[0].val > 0) {
        int npts = 1;
        adj_delta[i] += qp[0].delta;
        for (int j = 1; j < 3; j++) {
          if (qp[j].val > 0 && Math.abs(qp[j].delta - qp[0].delta) < 5) {
            npts++;
            adj_delta[i] += qp[j].delta;
          } else {
            break;
          }
        }
        adj_delta[i] /= npts;
        adj_delta_map.put(spm.spectrumId, adj_delta[i]);
      }
      ms1traces.add(qp);
      if (calc_mz > max_MZ) {
        max_MZ = calc_mz;
      }
      if (rt > max_RT) {
        max_RT = rt;
      }
    }
    double div_RT = 5;
    double div_MZ = 200;
    double[][] corr = new double[(int) (Math.round(max_RT / div_RT)) + 2][
        (int) (Math.round(max_MZ / div_MZ)) + 2];
    double[][] cnts = new double[(int) (Math.round(max_RT / div_RT)) + 2][
        (int) (Math.round(max_MZ / div_MZ)) + 2];
    //build calibration profile
    for (int i = 0; i < sa.spms.size(); i++) {
      Spm spm = sa.spms.get(i);
      double calc_mass = spm.mCalcNeutral;
      double calc_mz = (calc_mass + spm.charge * MASS_H) / spm.charge;
      double rt = spm.rtSec / 60;
      QuantPair[] qp = ms1traces.get(i);

      if (sa.fdr[i] <= 0.01) {
        if (Math.abs(spm.mDiff) < 0.1 && qp[0].val > 0) {
          double diff_ppm = 1000000 * ((spm.mDiff / spm.charge) / calc_mz);
          double adj_ppm = diff_ppm + adj_delta[i];
          if (Math.abs(adj_ppm) < 10) {
            int rti = (int) (rt / div_RT);
            int mzi = (int) (calc_mz / div_MZ);
            double dr = (rt - rti * div_RT) / div_RT;
            double dm = (calc_mz - mzi * div_MZ) / div_MZ;
            double[] dx = {1 - dr, dr};
            double[] dy = {1 - dm, dm};
            for (int x = 0; x < 2; x++) {
              for (int y = 0; y < 2; y++) {
                corr[rti + x][mzi + y] += dx[x] * dy[y] * adj_ppm;
                cnts[rti + x][mzi + y] += dx[x] * dy[y];
              }
            }
          }
        }
      }
    }
    for (int i = 0; i < corr.length; i++) {
      for (int j = 0; j < corr[i].length; j++) {
        if (cnts[i][j] > 0) {
          corr[i][j] /= cnts[i][j];
        }
      }
    }

    return new CalibrationData(div_RT, div_MZ, corr, adj_delta_map);
  }

  private void applyCalibration(List<Spm> spms, CalibrationData calData) {
    final double div_RT = calData.div_RT;
    final double div_MZ = calData.div_MZ;
    final double[][] corr = calData.corr;
    final HashMap<String, Double> adj_delta_map = calData.adj_delta_map;

    int countFound = 0;
    int countNotFound = 0;

    //apply profile
    double sum = 0;
    for (int i = 0; i < spms.size(); i++) {
      Spm spm = spms.get(i);
      double calc_mass = spm.mCalcNeutral;
      double calc_mz = (calc_mass + spm.charge * Table.Proton.monoMass) / spm.charge;
      double rt = spm.rtSec / 60;
      int rti = (int) (rt / div_RT);
      int mzi = (int) (calc_mz / div_MZ);
      double corrf = 0;
      double dr = (rt - rti * div_RT) / div_RT;
      double dm = (calc_mz - mzi * div_MZ) / div_MZ;
      double[] dx = {1 - dr, dr};
      double[] dy = {1 - dm, dm};
      for (int x = 0; x < 2; x++) {
        for (int y = 0; y < 2; y++) {
          corrf += dx[x] * dy[y] * corr[rti + x][mzi + y];
        }
      }

      double ppm_delta = -corrf;
      Double adj_delta = adj_delta_map.get(spm.spectrumId);
      if (adj_delta != null) {
        //throw new IllegalStateException("Could not find adj_delta for psm " + spm.spectrumId);
        countFound++;
        ppm_delta += adj_delta;
      } else {
        countNotFound++;
        //ppm_delta += 0.0;
        continue;
      }

      //double ppm_massdiff = 1000000.0*((sr.massdiff / sr.charge)/calc_mz);
			//System.out.printf("%.4f %.4f %.4f %.4f %.4f %.4f\n",rt,calc_mz, ppm_massdiff, ppm_massdiff + adj_delta[i], ppm_massdiff + ppm_delta, corrf);
      double mass_corr = (ppm_delta * calc_mz * spm.charge) / 1e6;
      sum += mass_corr;
      //out.printf("%s %.6f %.6f\n", sr.specName, sr.massdiff, sr.massdiff + mass_corr);
      spm.mDiffCorrected = spm.mDiff + mass_corr;

    }
    if (countNotFound > 0)
      log.info("Applied mass correction not to all peptide entries: {}/{}, avg mass_cor = {}", countFound, countNotFound + countFound, sum / countFound);
  }

  public static class CalibrationData implements Serializable {
    private static final long serialVersionUID = -7497899011001186998L;

    public final HashMap<String, Double> adj_delta_map;
    public final double div_RT;
    public final double div_MZ;
    public final double[][] corr;

    /**
     *
     * @param div_rt
     * @param div_mz
     * @param corr
     * @param adj_delta_map Key = spectrum id, Value = ppm correction to be applied to observed m/z.
     *    This is the value obtained by tracing MS1 peak.
     */
    public CalibrationData(double div_rt, double div_mz, double[][] corr,
        HashMap<String, Double> adj_delta_map) {
      this.adj_delta_map = adj_delta_map;
      div_RT = div_rt;
      div_MZ = div_mz;
      this.corr = corr;
    }
  }
}
