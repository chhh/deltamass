package umich.opensearch.kde.params.denoise;

import com.github.chhh.utils.dsp.denoise.DenoiseUtils;
import java.util.List;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import umich.opensearch.kde.jfree.JFreeChartPlot;

/**
 * @author Dmitry Avtonomov
 */
public class FusedLassoDenoiser implements Denoiser<FusedLassoDenoiser.Config> {

  private FusedLassoDenoiser() {
  }

  public static FusedLassoDenoiser getInstance() {
    return Singleton.instance;
  }

  @Override
  public void denoise(double[] origin, double[] target, NumberedParams params) {
    if (!(params instanceof Config)) {
      throw new IllegalArgumentException("Params must be of FusedLassoDenoiser.Config class");
    }
    if (origin.length != target.length) {
      throw new IllegalArgumentException("Array sizes must be equal");
    }
    FusedLassoDenoiser.Config conf = (FusedLassoDenoiser.Config) params;

    final double LAMBDA = conf.getLambda();
    final double MU = conf.getMu();
    DenoiseUtils.fused_lasso(origin, target, origin.length, LAMBDA, MU);

    if (conf.isPlotDenoising()) {
      JFreeChartPlot plot = new JFreeChartPlot("Fused Lasso denoising");
      String datasetKey = "TotalVar";
      String seriesOrigKey = "Original";
      String seriesDenoisedKey = String.format("Denoised (lambda: %.3f, mu:%.3f)", LAMBDA, MU);
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
      plot.display(JFreeChartPlot.ChartCloseOption.DISPOSE.windowConstant, "Denoise results");
    }
  }


  @Override
  public FusedLassoDenoiser.Config getDefaultConfig() {
    return new Config();
  }

  @Override
  public Config configure(List<Double> params) {
    if (params.size() != 2) {
      throw new IllegalArgumentException(
          "FusedLasso denoiser takes exactly 2 parameters for its configuration.");
    }
    Config config = new Config();
    config.setProperty(Config.PROP_LAMBDA, String.valueOf(params.get(0)));
    config.setProperty(Config.PROP_MU, String.valueOf(params.get(1)));
    return config;
  }

  @Override
  public int getNumberConfigParams() {
    return 2;
  }

  @Override
  public String getParamDescription(int i) {
    switch (i) {
      case 0:
        return "Lambda parameter (t1), the amount of penalty for fitted function variation.\n"
            + "Should be from 0 (in which case nothing is done to the signal) to any number.\n"
            + "See https://en.wikipedia.org/wiki/Lasso_%28statistics%29#Fused_lasso";
      case 1:
        return "Mu parameter (t2), the amount of allowed variation in coefficients [0-..]."
            + "See https://en.wikipedia.org/wiki/Lasso_%28statistics%29#Fused_lasso";
    }
    throw new IllegalArgumentException("Only 0 is the allowed argument.");
  }

  private static class Singleton {

    private static final FusedLassoDenoiser instance = new FusedLassoDenoiser();
  }

  public static class Config extends NumberedParams {

    public static String PROP_LAMBDA = "PROP_LAMBDA";
    public static String PROP_MU = "PROP_MU";
    public static String PROP_DO_PLOT = "PROP_DO_PLOT";

    public double getLambda() {
      return Double.parseDouble(getProperty(Config.PROP_LAMBDA, "0.05"));
    }

    public double getMu() {
      return Double.parseDouble(getProperty(PROP_MU, "0.01"));
    }

    public boolean isPlotDenoising() {
      return Boolean.parseBoolean(getProperty(PROP_DO_PLOT, "false"));
    }

    @Override
    public int getNumberOfParameters() {
      return 2;
    }

    @Override
    public double getParameter(int i) {
      switch (i) {
        case 0:
          return getLambda();
        case 1:
          return getMu();
      }
      throw new IllegalArgumentException("Parameter number can be 0 or 1 for FusedLasso Denoiser");
    }
  }
}
