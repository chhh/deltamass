package umich.opensearch.kde.pepxml;

/**
 * @author Dmitry Avtonomov
 */
public class SearchHitCount {

  public final int forwards;
  public final int decoys;

  public SearchHitCount(int forwards, int decoys) {
    this.forwards = forwards;
    this.decoys = decoys;
  }

  public int getTotal() {
    if ((long) forwards + (long) decoys > Integer.MAX_VALUE) {
      throw new RuntimeException(
          "Total number of forward and decoy hits was larger than Integer.MAX_VALUE");
    }
    return forwards + decoys;
  }
}
