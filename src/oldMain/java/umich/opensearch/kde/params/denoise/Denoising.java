package umich.opensearch.kde.params.denoise;

/**
 * @author Dmitry Avtonomov
 */
public enum Denoising {
  NONE(NoOpDenoiser.getInstance()),
  TOTAL_VARIATION(TotalVariationDenoiser.getInstance()),
  FUSED_LASSO(FusedLassoDenoiser.getInstance());


  private Denoiser<? extends NumberedParams> denoiser;

  Denoising(Denoiser<? extends NumberedParams> denoiser) {
    this.denoiser = denoiser;
  }

  public Denoiser<? extends NumberedParams> getInstance() {
    return denoiser;
  }
}
