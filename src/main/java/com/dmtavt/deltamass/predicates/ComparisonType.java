package com.dmtavt.deltamass.predicates;

import java.util.function.Function;

public enum ComparisonType {
  LESS("<"),
  LESS_OR_EQUAL("<="),
  EQUAL("=="),
  GREATER_OR_EQUAL(">="),
  GREATER(">");

  public final String symbol;

  ComparisonType(String symbol) {
    this.symbol = symbol;
  }

  public static ComparisonType fromSymbol(String s) {
    switch (s) {
      case "<":
        return LESS;
      case "<=":
        return LESS_OR_EQUAL;
      case "==":
        return EQUAL;
      case ">=":
        return GREATER_OR_EQUAL;
      case ">":
        return GREATER;
      default:
        throw new IllegalArgumentException("Unknown comparison symbol: " + s);
    }
  }

  public static Function<Double, Boolean> create(ComparisonType cmp, final double compareTo) {
    switch (cmp) {
      case LESS:
        return val -> Double.compare(val, compareTo) < 0;
      case LESS_OR_EQUAL:
        return val -> Double.compare(val, compareTo) <= 0;
      case EQUAL:
        return val -> Double.compare(val, compareTo) == 0;
      case GREATER_OR_EQUAL:
        return val -> Double.compare(val, compareTo) >= 0;
      case GREATER:
        return val -> Double.compare(val, compareTo) > 0;
      default:
        throw new IllegalStateException("Illegal comparison type for Doubles: " + cmp.symbol);
    }
  }

  public static Function<Integer, Boolean> create(ComparisonType cmp, final int compareTo) {
    switch (cmp) {
      case LESS:
        return val -> val < compareTo;
      case LESS_OR_EQUAL:
        return val -> val <= compareTo;
      case EQUAL:
        return val -> val == compareTo;
      case GREATER_OR_EQUAL:
        return val -> val >= compareTo;
      case GREATER:
        return val -> val > compareTo;
      default:
        throw new IllegalStateException("Illegal comparison type for Integers: " + cmp.symbol);
    }
  }

  public static Function<String, Boolean> create(ComparisonType cmp, final String compareTo) {
    if (compareTo == null) {
      throw new IllegalArgumentException("No nulls");
    }
    switch (cmp) {
      case EQUAL:
        return val -> compareTo.equals(val);
      default:
        throw new IllegalStateException("Illegal comparison type for Strings: " + cmp.symbol);
    }
  }

  @Override
  public String toString() {
    return symbol;
  }
}
