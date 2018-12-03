package umich.opensearch.kde;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dmitry Avtonomov
 */
public class KDEBin {

  public static final String PEAKS_DATASET_NAME = "Detected Peaks";
  private static final Logger log = LoggerFactory.getLogger(KDEBin.class);

//    public static void main(String[] args) {
////        if (args.length != 1)
////            System.err.println("The input must be exactly one existing file path");
////        Path path = Paths.get(args[0]).toAbsolutePath();
////        if (Files.notExists(path))
////            System.err.printf("File does not exist: %s", path);
//
//        LogHelper.configureJavaUtilLogging();
//        KDEBin kdeBin = new KDEBin();
//        KdeBinParams params = KDEBin.parseParams(args, System.err);
//        if (params == null)
//            return;
//
//        kdeBin.runKde(params, System.out);
//    }
//
//    public static KdeBinParams parseParams(String[] args, Appendable out) throws ParameterException {
//        log.debug("Program {} started", KdeBinParams.APP_NAME);
//        KdeBinParams params = new KdeBinParams();
//        JCommander jcom = new JCommander(params);
//        try {
//            jcom.setProgramName(KdeBinParams.APP_NAME);
//            if (args.length == 0) {
//                jcom.usage();
//                return null;
//            }
//            jcom.parse(args);
//            if (params.isHelp()) {
//                jcom.usage();
//                return null;
//            }
//
//            params.validate();
//
//        } catch (ParameterException pe) {
//            if (!params.isRunGui()) {
//                LogUtils.println(out, pe.getMessage());
//                return null;
//            }
//        }
//        return params;
//    }
//    private static boolean isBinary(Path path) {
//        return path.toString().endsWith(".bin");
//    }
//
//    private double[][] readData(Path path, boolean isBinary, KdeBinParams params) {
//        boolean dataHasWeights = params.hasWeights || params.colNumWeight != null;
//        if (!dataHasWeights) {
//            if (isBinary) {
//                int numDataPoints = (int) (path.toFile().length() / 8);
//                double[] result = new double[numDataPoints];
//                try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
//                    double val;
//                    int i = 0;
//                    while (true) {
//                        val = dis.readDouble();
//                        result[i] = val;
//                        i++;
//                    }
//                } catch (EOFException e) {
//                    // simply reached the end of file, all fine
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                return new double[][]{result};
//            } else {
//                final int colNumData = params.colNumData;
//                ArrayList<Double> listVals = new ArrayList<>((int) 1e6);
//                try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
//                    double val;
//                    String line;
//                    String[] split;
//                    Pattern delimiterRegEx = Pattern.compile(params.getDelimiterRegEx());
//                    if (params.hasHeaderRow()) {
//                        line = reader.readLine();
//                    }
//                    while ((line = reader.readLine()) != null) {
//                        split = delimiterRegEx.split(line);
//                        val = Double.parseDouble(split[colNumData]);
//                        listVals.add(val);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                double[] result = new double[listVals.size()];
//                for (int i = 0; i < listVals.size(); i++)
//                    result[i] = listVals.get(i);
//                return new double[][]{result};
//            }
//        } else {
//            if (isBinary) {
//                int numDataPoints = (int) ((path.toFile().length() / 8) / 2);
//                double[] result = new double[numDataPoints];
//                double[] weights = new double[numDataPoints];
//                try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
//                    double val;
//                    int i = 0;
//                    while (true) {
//                        val = dis.readDouble();
//                        result[i] = val;
//                        val = dis.readDouble();
//                        weights[i] = val;
//                        i++;
//                    }
//                } catch (EOFException e) {
//                    // simply reached the end of file, all fine
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                return new double[][]{result};
//            } else {
//                ArrayList<Double> listVals = new ArrayList<>((int) 1e6);
//                ArrayList<Double> listWeights = new ArrayList<>((int) 1e6);
//                final int colNumData = params.colNumData;
//                final int colNumWeight = params.colNumWeight;
//                try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
//                    double val;
//                    String line;
//                    String[] split;
//                    Pattern delimiterRegEx = Pattern.compile(params.getDelimiterRegEx());
//                    if (params.hasHeaderRow()) {
//                        line = reader.readLine();
//                    }
//                    while ((line = reader.readLine()) != null) {
//                        split = delimiterRegEx.split(line);
//                        val = Double.parseDouble(split[colNumData]);
//                        listVals.add(val);
//                        val = Double.parseDouble(split[colNumWeight]);
//                        listWeights.add(val);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                if (listVals.size() != listWeights.size())
//                    throw new IllegalStateException("Resulting lists must be of the same size");
//                double[] vals = new double[listVals.size()];
//                double[] weights = new double[listWeights.size()];
//                for (int i = 0; i < listWeights.size(); i++) {
//                    vals[i] = listVals.get(i);
//                    weights[i] = listWeights.get(i);
//                }
//                return new double[][]{vals, weights};
//            }
//        }
//    }
//
//
//
//    private void runKde(KdeBinParams params, Appendable out) {
//        String msg;
//        LogUtils.println(out, "Running Kernel Density Estimation");
//        log.debug("Running Kernel Density Estimation");
//
//        List<Path> matchingPaths = KDEMain.findMatchingPaths(params);
//        msg = String.format("Found %d files matching input (single files and '%s' pattern) in '%s'",
//                matchingPaths.size(), params.getInFileRegex(), StringUtils.join(params.getInFilePath(), ", "));
//        log.info(msg);
//        LogUtils.println(out, msg);
//        if (matchingPaths.size() == 0) {
//            msg = "No files found - nothing to do, exiting.";
//            log.info(msg);
//            LogUtils.println(out, msg);
//            return;
//        }
//
//
//        msg = "Preparing data for KDE";
//        log.info(msg);
//        LogUtils.println(out, msg);
//
//        List<Path> inFilePaths = params.getInFilePath();
//        ArrayList<double[][]> inputData = new ArrayList<>(inFilePaths.size());
//        for (Path inFilePath : inFilePaths) {
//            double[][] data = readData(inFilePath, isBinary(inFilePath), params);
//            inputData.add(data);
//        }
//
//        double[] massDiffs;
//        double[] weights = null;
//        if (inputData.size() == 1) {
//            massDiffs = inputData.get(0)[0];
//            if (params.hasWeights())
//                weights = inputData.get(0)[1];
//        } else {
//            int totalSize = 0;
//            for (double[][] a : inputData)
//                totalSize += a[0].length;
//            massDiffs = new double[totalSize];
//            if (params.hasWeights())
//                weights = new double[totalSize];
//            int curStartingPos = 0;
//            for (double[][] a : inputData) {
//                System.arraycopy(a[0], 0, massDiffs, curStartingPos, a[0].length);
//                if (params.hasWeights())
//                    System.arraycopy(a[1], 0, weights, curStartingPos, a[1].length);
//                curStartingPos += a.length;
//            }
//        }
//
//        int totalEntries = massDiffs.length;
//        if (totalEntries < 10) {
//            msg = "There were less than 10 entries in the parsed data set. Won't run KDE on such small data.";
//            log.info(msg);
//            LogUtils.println(out, msg);
//        }
//
//        DenseVector massDiffVec = new DenseVector(massDiffs);
//        KDEKludge kde;
//        msg = "Setting up KDE, might take a while..";
//        log.info(msg);
//        LogUtils.println(out, msg);
//        if (weights == null)
//            kde = new KDEKludge(massDiffVec, params.getKernelType().kernel);
//        else
//            kde = new KDEKludge(massDiffVec, params.getKernelType().kernel, weights);
//
//        double kdeMin = kde.min();
//        double kdeMax = kde.max();
//        msg = String.format("KDE's PDF support is: [%.2f; %.2f], number of points %d.", kdeMin, kdeMax, massDiffVec.length());
//        log.info(msg);
//        LogUtils.println(out, msg);
//
//
//        // figuring out bandwidths
//        List<NamedBandwidth> namedBandwidths = params.getNamedBandwidths();
//        if (namedBandwidths.isEmpty()) {
//            log.error("Bandwidths list was empty!");
//        }
//
//
//        // setting up axes
//        double lo = params.getMzLo() == null ? kdeMin : params.getMzLo();
//        double hi = params.getMzHi() == null ? kdeMax : params.getMzHi();
//        double step = params.getMzStep();
//        int numPts = (int) ((hi - lo) / step) + 1;
//        if (numPts < 2) {
//            throw new IllegalStateException("For some reason x axis only contained 2 points, proably incorrect parameters");
//        }
//        double[] xAxis = new double[numPts];
//        xAxis[0] = lo;
//        for (int i = 1; i < xAxis.length; i++) {
//            xAxis[i] = xAxis[i - 1] + step;
//        }
//        String datasetId = "KDE series";
//        JFreeChartPlot plot = new JFreeChartPlot("KDE Open Search");
//        try {
//            Mods mods = PtmFactory.getMods();
//            plot.setMods(mods);
//        } catch (ModParsingException e) {
//            log.error("Could not parse modifications from different sources", e);
//            LogUtils.println(out, "Error parsing modification data");
//            System.exit(1);
//        }
//
//        YIntervalSeriesCollection datasetPeaks = new YIntervalSeriesCollection();
//
//        if (params.isDynamicBandwidthEstimation()) {
//            doKdeForDynBandwidth(out, params, namedBandwidths,
//                    massDiffVec, kde, lo, hi, step,
//                    xAxis, datasetId, plot, numPts, totalEntries, datasetPeaks);
//        }
//        for (NamedBandwidth bandwidth : namedBandwidths) {
//            doKdeForOtherBandwiths(out, params, bandwidth,
//                    massDiffVec, kde, lo, hi, step,
//                    xAxis, datasetId, plot, numPts, totalEntries, datasetPeaks);
//        }
//
//
//        // plotting first derivative
//        if (false) {
//            double h = 0.001;
//            String seriesIdDer = "der2@0.001h";
//
//            msg = String.format("Calcing KDE for dataset: %s\n", seriesIdDer);
//            log.info(msg);
//            LogUtils.println(out, msg);
//
//            kde.setBandwith(h);
//            kde.setKernelFunction(GaussFasterKF.getInstance());
//            double cur = lo, val;
//            int curIdx = 0;
//            double integralPdf = 0.0d;
//            double[] yAxis = new double[numPts];
//            while (cur <= hi) {
//                val = kde.pdfPrime2(cur);
//                yAxis[curIdx] = val;
//                integralPdf += val * step;
//                curIdx++;
//                cur += step;
//            }
//            msg = String.format("\tTotal area under KDEder : %.4f\n", integralPdf);
//            log.info(msg);
//            LogUtils.println(out, msg);
//            ArrayFilter zeroFilterDer = new ArrayFilter(xAxis, yAxis);
//            zeroFilterDer = zeroFilterDer.filterZeroesLeaveFlanking();
//            plot.addSeries(datasetId, seriesIdDer, zeroFilterDer.getMass(), zeroFilterDer.getInts());
//        }
//
//
//        msg = "Generating plot";
//        log.info(msg);
//        LogUtils.println(out, msg);
//
//        if (params.isPlotKde()) {
//            if (datasetPeaks.getSeriesCount() > 0) // if we were adding detected peaks, then plot them
//                plot.addDataset(datasetPeaks, new YIntervalRenderer());
//            plot.display();
//            plot.addMouseListener(Collections.<PepXmlContent>emptyList());
//        }
//
//        log.debug("Plotting KDE finished");
//    }
//
//    private void doKdeForOtherBandwiths(Appendable out, KdeBinParams params, NamedBandwidth bandwidth, DenseVector massDiffVec, KDEKludge kde, double lo, double hi, double step, double[] xAxis, String datasetId, JFreeChartPlot plot, int numPts, int totalEntries, YIntervalSeriesCollection datasetPeaks) {
//        String msg;
//        double h;
//        String seriesId;
//
//        if (bandwidth.isDynamic() && bandwidth.getTargetMz() != null) {
//            // dynamic estimation at prescribed m/z
//            h = KDEUtils.estimateBandwidth(massDiffVec, params.getAutoBandwithTarget(), params.getAutoBandwidthWindow());
//            seriesId = String.format("h=%.4f (est. @ %.2f+/-%.2f)", h, params.getAutoBandwithTarget(), params.getAutoBandwidthWindow());
//        } else {
//            h = bandwidth.getBandwidth();
//            seriesId = String.format("h=%.4f", h);
//        }
//
//        msg = String.format("Calcing KDE for dataset: %s\n", seriesId);
//        log.info(msg);
//        LogUtils.println(out, msg);
//
//        kde.setBandwith(h);
//        double cur = lo, val;
//        int curIdx = 0;
//        double integralPdf = 0.0d;
//        double[] yAxis = new double[numPts];
//        while (cur <= hi && curIdx < yAxis.length) {
//            val = kde.pdf(cur);
//            yAxis[curIdx] = val;
//            integralPdf += val * step;
//            curIdx++;
//            cur += step;
//        }
//        addSeries(out, xAxis, yAxis, plot, params, datasetId, seriesId, integralPdf, totalEntries);
//        if (params.isDetectPeaks()) {
//            KDEMain.detectPeaks(out, params, xAxis, datasetPeaks, seriesId, bandwidth, yAxis);
//        }
//    }
//
//    private void doKdeForDynBandwidth(Appendable out, KdeBinParams params, List<NamedBandwidth> namedBandwidths, DenseVector massDiffVec, KDEKludge kde, double lo, double hi, double step, double[] xAxis, String datasetId, JFreeChartPlot plot, int numPts, int totalEntries, YIntervalSeriesCollection datasetPeaks) {
//        String msg;
//        NamedBandwidth dynamicBandwidth = null;
//
//        // ugly, but works. removing the dynamic bandwith estimation entry from the list of bandwidths.
//        for (int i = 0; i < namedBandwidths.size(); i++) {
//            dynamicBandwidth = namedBandwidths.get(i);
//            if (dynamicBandwidth.isBandwidthAuto()) {
//                namedBandwidths.remove(i);
//                break;
//            }
//        }
//        long nomMassPrev = Long.MIN_VALUE;
//        long nomMassCur = nomMassPrev;
//        String seriesId = dynamicBandwidth.getName();
//        msg = String.format("Calcing KDE for series '%s'", seriesId);
//        log.info(msg);
//        LogUtils.println(out, msg);
//
//        double cur = lo, val;
//        int curIdx = 0;
//        double integralPdf = 0;
//        double[] yAxis = new double[numPts];
//        while (cur <= hi && curIdx < yAxis.length) {
//            nomMassCur = Math.round(cur);
//            if (nomMassCur != nomMassPrev) {
//                // recalc bandwidth
//                double bandwidthAtThisNomMass = KDEUtils.estimateBandwidth(massDiffVec, (double) nomMassCur, params.getAutoBandwidthWindow());
//                if (bandwidthAtThisNomMass == 0 || Double.isInfinite(bandwidthAtThisNomMass) || Double.isNaN(bandwidthAtThisNomMass)) {
//                    bandwidthAtThisNomMass = 0.5;
//                }
//                kde.setBandwith(bandwidthAtThisNomMass);
//                nomMassPrev = nomMassCur;
//            }
//            val = kde.pdf(cur);
//            yAxis[curIdx] = val;
//            integralPdf += val * step;
//            curIdx++;
//            cur += step;
//        }
//        addSeries(out, xAxis, yAxis, plot, params, datasetId, seriesId, integralPdf, totalEntries);
//        if (params.isDetectPeaks()) {
//            KDEMain.detectPeaks(out, params, xAxis, datasetPeaks, seriesId, dynamicBandwidth, yAxis);
//        }
//    }
//
//    private void addSeries(Appendable out, double[] xAxis, double[] yAxis,
//                           JFreeChartPlot plot, KdeBinParams params, String datasetId, String seriesId,
//                           double integralPdf, int totalEntries) {
//        String msg;
//        msg = String.format("\tTotal area under KDE: %.4f\n", integralPdf);
//        log.info(msg);
//        LogUtils.println(out, msg);
//
////        Denoiser<? extends NumberedParams> denoiser = params.getDenoising().getInstance();
////        NumberedParams denoiserProperties = denoiser.getDefaultConfig();
////        denoiserProperties.put("massAxis", xAxis);
////
////        denoiserProperties.put(TotalVariationDenoiser.Config.PROP_LAMBDA, String.valueOf(params.getDenoisingParams()));
////        denoiserProperties.put(TotalVariationDenoiser.Config.PROP_DO_PLOT, String.valueOf(params.isDenoisingPlot()));
////        double[] denoised = new double[yAxis.length];
////        denoiser.denoise(yAxis, denoised, denoiserProperties);
//
//        ArrayFilter zeroFilter = new ArrayFilter(xAxis, yAxis);
//        zeroFilter = zeroFilter.filterZeroesLeaveFlanking();
//
//        plot.addSeries(datasetId, seriesId, zeroFilter.getMass(), zeroFilter.getInts());
//    }
}
