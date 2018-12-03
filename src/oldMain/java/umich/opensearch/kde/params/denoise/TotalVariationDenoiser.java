package umich.opensearch.kde.params.denoise;

import com.github.chhh.utils.dsp.denoise.DenoiseUtils;
import java.util.List;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import umich.opensearch.kde.jfree.JFreeChartPlot;

/**
 * @author Dmitry Avtonomov
 */
public class TotalVariationDenoiser implements Denoiser<TotalVariationDenoiser.Config> {

  private TotalVariationDenoiser() {
  }

  public static TotalVariationDenoiser getInstance() {
    return Singleton.instance;
  }

  @Override
  public void denoise(double[] origin, double[] target, NumberedParams params) {
    if (!(params instanceof Config)) {
      throw new IllegalArgumentException("Params must be of TotalVariationDenoiser.Config class");
    }
    if (origin.length != target.length) {
      throw new IllegalArgumentException("Array sizes must be equal");
    }
    TotalVariationDenoiser.Config conf = (TotalVariationDenoiser.Config) params;

    long time1 = System.nanoTime();
    final double LAMBDA = conf.getLambda();
    DenoiseUtils.TV1D_denoise(origin, target, origin.length, LAMBDA);
    long time2 = System.nanoTime();
    //System.out.printf("computation time: %.5fs\n",(double)(time2 - time1) / 1e9);

    if (conf.isPlotDenoising()) {
      JFreeChartPlot plot = new JFreeChartPlot("Total variation denoising");
      String datasetKey = "TotalVar";
      String seriesOrigKey = "Original";
      String seriesDenoisedKey = String.format("Denoised, lambda=%.3f", LAMBDA);
      double[] xAxis = (double[]) params.get("massAxis");

      XYDataset dataset = plot.getDataset(datasetKey);
      if (dataset == null) {
        dataset = new DefaultXYDataset();
        plot.addDataset(datasetKey, dataset, false);
      }
      if (dataset instanceof DefaultXYDataset) {
        DefaultXYDataset xyDataset = (DefaultXYDataset) dataset;
        xyDataset.addSeries(seriesOrigKey, new double[][]{xAxis, origin});
        xyDataset.addSeries(seriesDenoisedKey, new double[][]{xAxis, target});
      }
      plot.display(JFreeChartPlot.ChartCloseOption.DISPOSE.windowConstant,
          "Total Variation Denoise");
    }
  }


  //sort the array, and return the median
  private int median(int[] a) {
    int temp;
    int asize = a.length;
    //sort the array in increasing order
    for (int i = 0; i < asize; i++) {
      for (int j = i + 1; j < asize; j++) {
        if (a[i] > a[j]) {
          temp = a[i];
          a[i] = a[j];
          a[j] = temp;
        }
      }
    }
    //if it's odd
    if (asize % 2 == 1) {
      return a[asize / 2];
    } else {
      return ((a[asize / 2] + a[asize / 2 - 1]) / 2);
    }
  }

  @Override
  public TotalVariationDenoiser.Config getDefaultConfig() {
    return new Config();
  }

  @Override
  public Config configure(List<Double> params) {
    if (params.size() != 1) {
      throw new IllegalArgumentException(
          "TotalVariation denoiser takes exactly 1 parameter for its configuration.");
    }
    Config config = new Config();
    config.setProperty(Config.PROP_LAMBDA, String.valueOf(params.get(0)));
    return config;
  }

  @Override
  public int getNumberConfigParams() {
    return 1;
  }

  @Override
  public String getParamDescription(int i) {
    if (i != 0) {
      throw new IllegalArgumentException("Only 0 is the allowed argument.");
    }
    return "Lambda parameter, the amount of penalty for fitted function variation. "
        + "Should be from 0 (in which case nothing is done to the signal) to any number.\n"
        + "See https://en.wikipedia.org/wiki/Total_variation_denoising";
  }

  private static class Singleton {

    private static final TotalVariationDenoiser instance = new TotalVariationDenoiser();
  }

  public static class Config extends NumberedParams {

    public static String PROP_LAMBDA = "PROP_LAMBDA";
    public static String PROP_DO_PLOT = "PROP_DO_PLOT";

    public double getLambda() {
      return Double.parseDouble(getProperty(Config.PROP_LAMBDA, "0.5"));
    }

    public boolean isPlotDenoising() {
      return Boolean.parseBoolean(getProperty(PROP_DO_PLOT, "false"));
    }

    @Override
    public int getNumberOfParameters() {
      return 1;
    }

    @Override
    public double getParameter(int i) {
      switch (i) {
        case 0:
          return getLambda();
      }
      throw new IllegalArgumentException(
          "Only 0 is accepted as parameter number for TotalVariation Denoiser.");
    }
  }
}
