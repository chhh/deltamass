package com.dmtavt.deltamass.predicates;

import com.dmtavt.deltamass.data.Spm;
import java.util.function.Function;

public class IntSpmPredicate implements ISpmPredicate {

  private final String property;
  private final ComparisonType cmp;
  private final int compareTo;
  private final Function<Spm, Integer> fetcher;
  private final Function<Integer, Boolean> comparisonDelegate;

  public IntSpmPredicate(String property, ComparisonType cmp, String comparisonStr,
      Function<Spm, Integer> fetcher) {
    this(property, cmp, Integer.parseInt(comparisonStr), fetcher);
  }

  public IntSpmPredicate(String property, ComparisonType cmp, int compareTo, Function<Spm, Integer> fetcher) {
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

}
