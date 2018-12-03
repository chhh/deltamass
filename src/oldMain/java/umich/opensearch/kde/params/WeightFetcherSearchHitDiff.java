package umich.opensearch.kde.params;

import java.util.Map;
import umich.opensearch.kde.api.SearchHitDiff;

/**
 * @author Dmitry Avtonomov
 */
public class WeightFetcherSearchHitDiff implements IWeightFetcher<SearchHitDiff> {

  private final int index;

  private WeightFetcherSearchHitDiff(int index) {
    this.index = index;
  }

  @Override
  public double fetch(SearchHitDiff searchHitDiff) {
    return searchHitDiff.getScores()[index];
  }

  public static class Factory {

    String weightScoreName;

    public Factory(String weightScoreName) {
      this.weightScoreName = weightScoreName;
    }

    public WeightFetcherSearchHitDiff create(Map<String, Integer> map) {
      Integer index = map.get(weightScoreName);

      return new WeightFetcherSearchHitDiff(index);
    }
  }
}
