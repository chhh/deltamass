package umich.opensearch.kde.params.predicates;

/**
 * @author Dmitry Avtonomov
 */
public enum ComparisonType {
  LESS(Symbols.LESS),
  LESS_OR_EQUAL(Symbols.LESS_OR_EQUAL),
  EQUAL(Symbols.EQUAL),
  GREATER_OR_EQUAL(Symbols.GREATER_OR_EQUAL),
  GREATER(Symbols.GREATER);

  public final String symbol;

  ComparisonType(String symbol) {
    this.symbol = symbol;
  }

  public static ComparisonType fromString(String s) {
    switch (s) {
      case Symbols.LESS:
        return LESS;
      case Symbols.LESS_OR_EQUAL:
        return LESS_OR_EQUAL;
      case Symbols.EQUAL:
        return EQUAL;
      case Symbols.GREATER_OR_EQUAL:
        return GREATER_OR_EQUAL;
      case Symbols.GREATER:
        return GREATER;
      default:
        throw new IllegalArgumentException(
            String.format("Unknown string given for parsing as a comparison type: '%s'", s));
    }
  }

  @Override
  public String toString() {
    return symbol;
  }
}
