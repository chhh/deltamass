package umich.opensearch.kde.params.predicates;

import com.github.chhh.utils.MathUtils;
import umich.opensearch.kde.api.SearchHitDiff;

/**
 * @author Dmitry Avtonomov
 */
public class MassDiffRangePredicate implements NamedPredicate<SearchHitDiff> {

  final double mLo;
  final double mHi;
  final double[][] exclude;

  public MassDiffRangePredicate(double mLo, double mHi, double[][] exclusionRnages) {
    if (mLo > mHi) {
      throw new IllegalArgumentException("mLo must be lower or equal to mHi");
    }
    this.mLo = mLo;
    this.mHi = mHi;
    this.exclude = exclusionRnages;
  }

  @Override
  public boolean apply(SearchHitDiff searchHitDiff) {
    if (!MathUtils.isBetweenOrEqual(searchHitDiff.getMassDiff(), mLo, mHi)) {
      return false;
    }
    if (exclude != null) {
      for (int i = 0; i < exclude.length; i++) {
        if (searchHitDiff.getMassDiff() >= exclude[i][0]
            && searchHitDiff.getMassDiff() <= exclude[i][1]) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Mass difference in range [%.4f; %.4f]", mLo, mHi));
    if (exclude != null && exclude.length > 0) {
      sb.append(", but not in ");
      for (int i = 0; i < exclude.length; i++) {
        sb.append(String.format("[%.4f; %.4f]", exclude[i][0], exclude[i][1]));
        if (i != exclude.length - 1) {
          sb.append(", ");
        }
      }
    }

    return sb.toString();
  }
}
