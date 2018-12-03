package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.data.PepSearchFile;
import com.dmtavt.deltamass.data.PepSearchResult;
import com.dmtavt.deltamass.data.PepSearchResult.SortOrder;
import com.dmtavt.deltamass.data.Spm;
import com.dmtavt.deltamass.kde.GaussFasterKF;
import com.dmtavt.deltamass.kde.KdeKludge;
import com.dmtavt.deltamass.utils.GridUtils;
import com.github.chhh.utils.search.BinarySearch;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jsat.distributions.empirical.KernelDensityEstimator;
import jsat.linear.DenseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicKde {

  private static final Logger log = LoggerFactory.getLogger(LogicKde.class);
  private static final DecimalFormat df2 = new DecimalFormat("0.00");
  private static final DecimalFormat df3 = new DecimalFormat("0.000)");

  private final UserOptsKde opts;
  private final List<PepSearchFile> pepSearchFiles;

  LogicKde(UserOptsKde opts, List<PepSearchFile> pepSearchFiles) {
    this.opts = opts;
    this.pepSearchFiles = pepSearchFiles;
  }

  public List<ProcessingSegment> run() {

    // make sure input data is sorted
    log.info("Collecting and sorting input data for KDE.");
    pepSearchFiles.stream()
        .flatMap(psf -> psf.pepSearchResults.stream())
        .parallel().forEach(psr -> psr.sort(SortOrder.DELTA_MASS_ASCENDING));

    // Collect SPMs from all files into one structure,
    // wrap SPMs with meta info and sort by mass diff
    List<SpmInfo> spmInfos = pepSearchFiles.stream()
        .flatMap(f -> f.pepSearchResults.stream())
        .flatMap(r -> r.spms.stream()
            .map(spm -> new SpmInfo(spm, r)))
        .sorted(Comparator.comparingDouble(si -> si.spm.mDiffCorrected))
        .collect(Collectors.toList());
    if (spmInfos.isEmpty()) {
      log.error("No PSMs given for KDE. Maybe you filtered them all out?");
      throw new IllegalStateException("No PSMs given for KDE.");
    }

    List<DataSegment> data = segment(spmInfos, opts.minData);
    log.info("Prepared data for {} nominal mass data segments", data.size());

    ProcessingConfig conf = new ProcessingConfig(opts.step, opts.bandwidth);
    List<ProcessingSegment> processed = process(data, conf);

    return processed;
  }

  /**
   * @param minData If a segment contains less than
   */
  List<DataSegment> segment(List<SpmInfo> spmInfos, int minData) {
    // find delta mass bounds
    final double dmLo = spmInfos.get(0).spm.mDiffCorrected;
    final double dmHi = spmInfos.get(spmInfos.size() - 1).spm.mDiffCorrected;
    final double segSpan = 1.0;
    final double xLoBound = Math.round(dmLo) - segSpan / 2.0;
    final double xHiBound = Math.round(dmHi) + segSpan / 2.0;

    List<DataSegment> segments = new ArrayList<>();
    for (int i = (int) xLoBound; i <= (int) xHiBound; i++) {
      final double lo = i - segSpan / 2.0;
      final double hi = i + segSpan / 2.0;
      int searchLo = BinarySearch.search(spmInfos, si -> Double.compare(si.spm.mDiffCorrected, lo));
      int insertionLo = searchLo < 0 ? ~searchLo : searchLo;
      int searchHi = BinarySearch.search(spmInfos, si -> Double.compare(si.spm.mDiffCorrected, hi));
      int insertionHi = searchHi < 0 ? ~searchHi : searchHi;
      List<SpmInfo> sub = spmInfos.subList(insertionLo, insertionHi);
      if (sub.isEmpty()) {
        log.info("Skipping data range [{}; {}] - empty", df2.format(lo), df2.format(hi));
      } else if (sub.size() < minData) {
        log.info("Skipping data range [{}; {}] - less than {} data points", df2.format(lo),
            df2.format(hi), minData);
      } else {
        segments.add(new DataSegment(lo, hi, sub));
      }
    }
    return segments;
  }

  private List<ProcessingSegment> process(List<DataSegment> data, ProcessingConfig conf) {
    List<ProcessingSegment> processed = new ArrayList<>();
    for (DataSegment ds : data) {
      ProcessingSegment ps = new ProcessingSegment(ds);
      ps.process(conf);
      processed.add(ps);
    }
    return processed;
  }

  public static class SpmInfo {

    public final Spm spm;
    public final PepSearchResult psr;

    public SpmInfo(Spm spm, PepSearchResult psr) {
      this.spm = spm;
      this.psr = psr;
    }
  }

  public static class DataSegment {

    public final double xLoBracket;
    public final double xHiBracket;

    public final double xLoData;
    public final double xHiData;
    public final double xMeanData;
    public final List<SpmInfo> spmInfos;

    public DataSegment(double xLoBracket, double xHiBracket, List<SpmInfo> spmsInfos) {
      this.xLoBracket = xLoBracket;
      this.xHiBracket = xHiBracket;
      for (int i = 0, sz = spmsInfos.size() - 1; i < sz; i++) {
        if (spmsInfos.get(i + 1).spm.mDiffCorrected < spmsInfos.get(i).spm.mDiffCorrected)
          throw new IllegalArgumentException("Input list must be sorted ascending by SPM mDiffCorrected");
      }
      if (spmsInfos.isEmpty())
        throw new IllegalArgumentException("Input list can't be empty");

      this.xLoData = spmsInfos.get(0).spm.mDiffCorrected;
      this.xHiData = spmsInfos.get(spmsInfos.size()-1).spm.mDiffCorrected;
      this.xMeanData = spmsInfos.stream().mapToDouble(si -> si.spm.mDiffCorrected).average().orElseThrow(IllegalArgumentException::new);
      this.spmInfos = spmsInfos;
    }
  }

  public static class ProcessingSegment {

    public final DataSegment data;
    private boolean isProcessed;

    public double[] x;
    public double xStep;
    public double bandwidth = Double.NaN;
    public double[] kde;
    public double[] der2;
    public double kdeAuc;

    public ProcessingSegment(DataSegment data) {
      this.data = data;
      isProcessed = false;
    }

    public boolean isProcessed() { return isProcessed; }

    void process(ProcessingConfig config) {
      if (Thread.interrupted()) {
        throw new RuntimeException("Interrupted");
      }
      isProcessed = false;

      // kde data
      double[] kdeDataArr = new double[data.spmInfos.size()];
      for (int i = 0, sz = data.spmInfos.size(); i < sz; i++)
        kdeDataArr[i] = data.spmInfos.get(i).spm.mDiffCorrected;
      DenseVector kdeDataVec = new DenseVector(kdeDataArr);

      // bandwidth
      final double h = config.bandwidth > 0 && Double.isFinite(config.bandwidth)
          ? config.bandwidth
          : KernelDensityEstimator.BandwithGuassEstimate(kdeDataVec);
      bandwidth = h;

      // grid
      xStep = config.xStep;
      final double drawDistance = h * 2.0;
      final double gridLoBound = Math.max(data.xLoData - drawDistance, data.xLoBracket);
      final double gridHiBound = Math.min(data.xHiData + drawDistance, data.xHiBracket);
      x = GridUtils.grid(data.xMeanData, gridLoBound, gridHiBound, xStep, xStep/2);

      // kde estimate
      KdeKludge estimator = new KdeKludge(kdeDataVec, GaussFasterKF.getInstance(), h);
      kde = new double[x.length];
      der2 = new double[x.length];
      kdeAuc = 0;
      for (int i = 0; i < x.length; i++) {
        final double pdf = estimator.pdf(x[i]);
        kde[i] = pdf;
        kdeAuc += pdf;
        der2[i] = estimator.pdfPrime2(x[i]);
      }
      kdeAuc *= xStep;
      log.debug("Total KDE PDF area for range [{}; {}] is {}",
          df2.format(data.xLoBracket), df2.format(data.xHiBracket), df2.format(kdeAuc));
      isProcessed = true;
    }
  }



  protected static class ProcessingConfig {

    public final double xStep;
    public final double bandwidth;

    private ProcessingConfig(double xStep, double bandwidth) {
      this.xStep = xStep;
      this.bandwidth = bandwidth;
    }
  }
}
