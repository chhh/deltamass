package com.dmtavt.deltamass.predicates;

import com.dmtavt.deltamass.data.Spm;
import java.text.DecimalFormat;
import java.util.function.Function;

public class DoubleSpmPredicate implements ISpmPredicate {

  private final String property;
  private final ComparisonType cmp;
  private final double compareTo;
  private final Function<Spm, Double> fetcher;
  private final Function<Double, Boolean> comparisonDelegate;
  private static DecimalFormat format = new DecimalFormat("0.###E3");

  public DoubleSpmPredicate(String property, ComparisonType cmp, String comparisonStr, Function<Spm, Double> fetcher) {
    this(property, cmp, Double.parseDouble(comparisonStr), fetcher);
  }

  public DoubleSpmPredicate(String property, ComparisonType cmp, double compareTo, Function<Spm, Double> fetcher) {
    this.property = property;
    this.cmp = cmp;
    this.compareTo = compareTo;
    this.fetcher = fetcher;
    this.comparisonDelegate = ComparisonType.create(cmp, compareTo);
  }

  @Override
  public boolean test(Spm spm) {
    return comparisonDelegate.apply(fetcher.apply(spm));
  }

  @Override
  public String toString() {
    return "Spm Predicate: " + property + " " + cmp + " " + format.format(compareTo);
  }
}
