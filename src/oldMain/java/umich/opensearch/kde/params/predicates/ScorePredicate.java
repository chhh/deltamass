package umich.opensearch.kde.params.predicates;

import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.exceptions.ParsingException;
import com.google.common.base.Predicate;
import java.util.Map;
import umich.opensearch.kde.api.SearchHitDiff;

/**
 * @author Dmitry Avtonomov
 */
public class ScorePredicate implements NamedPredicate<SearchHitDiff> {

  final ComparisonType type;
  final String name;
  final double value;
  final private Predicate<Double> delegate;
  final private Integer index;

  /**
   * Use {@link Factory} instead. The reason is that the predicate needs to be configured, as it
   * doesn't know the right score index beforehand.
   *
   * @param type comparison type
   * @param name name of the score
   * @param value value of the score
   */
  protected ScorePredicate(ComparisonType type, String name, double value,
      Predicate<Double> delegate, Integer index) {
    this.type = type;
    this.name = name;
    this.value = value;
    this.delegate = delegate;
    this.index = index;
  }

  @Override
  public boolean apply(SearchHitDiff searchHitDiff) {
    return delegate.apply(searchHitDiff.getScores()[index]);
  }

  @Override
  public String getDescription() {
    return String.format("Score '%s' (ordinal #%d) %s %.4f", name, index, type.symbol, value);
  }

  public static class Factory {

    final ComparisonType type;
    final String name;
    final double value;
    private Predicate<Double> delegate;

    public Factory(ComparisonType type, String name, double value) {
      this.type = type;
      this.name = name;
      this.value = value;
      switch (type) {
        case LESS:
          delegate = new Predicate<Double>() {
            @Override
            public boolean apply(Double v) {
              return v < Factory.this.value;
            }
          };
          break;
        case LESS_OR_EQUAL:
          delegate = new Predicate<Double>() {
            @Override
            public boolean apply(Double v) {
              return v <= Factory.this.value;
            }
          };
          break;
        case EQUAL:
          delegate = new Predicate<Double>() {
            @Override
            public boolean apply(Double v) {
              return v == Factory.this.value;
            }
          };
          break;
        case GREATER_OR_EQUAL:
          delegate = new Predicate<Double>() {
            @Override
            public boolean apply(Double v) {
              return v >= Factory.this.value;
            }
          };
          break;
        case GREATER:
          delegate = new Predicate<Double>() {
            @Override
            public boolean apply(Double v) {
              return v > Factory.this.value;
            }
          };
          break;
      }


    }

    @Override
    public String toString() {
      return String.format("ScorePredicate Factory: {%s %s %s}", name, type.symbol, value);
    }

    /**
     * Creates the predicate. Use this instead of {@link ScorePredicate} constructor, which is
     * protected.
     *
     * @param map mapping from score names to score indexes. SearchHitDiffs must have such a
     * mapping.
     */
    public ScorePredicate create(Map<String, Integer> map) throws ParsingException {
      Integer idx = map.get(name);
      if (idx == null) {
        throw new ParsingException(String.format(
            "Mapping from score names to score indexes did not contain the name we searched for: '%s'. "
                +
                "Known mappings are: '%s'", name, StringUtils.join(map.keySet(), ", ")));
      }
      return new ScorePredicate(type, name, value, delegate, idx);
    }
  }
}
