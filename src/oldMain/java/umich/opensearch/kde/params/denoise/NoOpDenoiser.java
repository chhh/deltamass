package umich.opensearch.kde.params.denoise;

import java.util.List;

/**
 * @author Dmitry Avtonomov
 */
public class NoOpDenoiser implements Denoiser<NumberedParams> {

  private NoOpDenoiser() {
  }

  public static NoOpDenoiser getInstance() {
    return Singleton.instance;
  }

  @Override
  public void denoise(double[] origin, double[] target, NumberedParams params) {

  }

  @Override
  public NumberedParams getDefaultConfig() {
    return new NumberedParamsDefault();
  }

  @Override
  public NumberedParams configure(List<Double> params) {
    return getDefaultConfig();
  }

  @Override
  public int getNumberConfigParams() {
    return 0;
  }

  @Override
  public String getParamDescription(int i) {
    return "";
  }

  private static class Singleton {

    private static NoOpDenoiser instance = new NoOpDenoiser();
  }
}
