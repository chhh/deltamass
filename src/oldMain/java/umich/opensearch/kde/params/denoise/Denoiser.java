package umich.opensearch.kde.params.denoise;

import java.util.List;

/**
 * @param <T> parameter type for the denoising algo.
 */
public interface Denoiser<T extends NumberedParams> {

  /**
   * Apply denoising to a discrete signal. If target equals to origin then the denoising can be done
   * in-place, for some implementations you might be required to provide different arrays.
   *
   * @param origin the data to be denoised
   * @param target can be the same as origin, if the implementation supports in-place denoising
   * @param params this can, for example, be the result of {@link #getDefaultConfig() }
   */
  void denoise(double[] origin, double[] target, NumberedParams params);

  /**
   * Default properties to use with this denoiser.
   */
  T getDefaultConfig();

  /**
   * Initialize this denoiser's config with the provided parameters.
   *
   * @param params to find how many params are accepted check the {@link #getNumberConfigParams() }
   * method.
   */
  T configure(List<Double> params);

  int getNumberConfigParams();

  /**
   * Provides description for input parameters.
   *
   * @param i the ordinal number of the configuration parameter.
   * @return A short description of what the parameter does
   */
  String getParamDescription(int i);
}
