package com.dmtavt.deltamass.predicates;

import java.util.Map;

public class SpmPredicateFactory {

  private String property;
  private ComparisonType cmp;
  private String value;

  public SpmPredicateFactory(String property, ComparisonType cmp, String value) {
    this.property = property.toLowerCase();
    this.cmp = cmp;
    this.value = value;
  }

  @Override
  public String toString() {
    return property + " " + cmp + " " + value;
  }

  public ISpmPredicate create(Map<String, Integer> scoreMapping) throws ScoreNameNotExists {
    switch (property) {
      case "mobs":
        return new DoubleSpmPredicate(property, cmp, value, spm -> spm.mObsNeutral);
      case "mcalc":
        return new DoubleSpmPredicate(property, cmp, value, spm -> spm.mCalcNeutral);
      case "mdiff":
        return new DoubleSpmPredicate(property, cmp, value, spm -> spm.mDiff);
      case "mdiffcorr":
        return new DoubleSpmPredicate(property, cmp, value, spm -> spm.mDiffCorrected);
      case "mzobs":
        return new DoubleSpmPredicate(property, cmp, value, spm -> spm.mzObs);
      case "z":
        return new IntSpmPredicate(property, cmp, value, spm -> spm.charge);
      default:
        // other property names are considered "score names"
        Integer ordinal = scoreMapping.get(property);
        if (ordinal == null) {
          throw new ScoreNameNotExists("Sore mapping didn't contain name: '" + property + "'",
              property);
        }
        final int index = ordinal;
        return new DoubleSpmPredicate(property, cmp, value, spm -> spm.scores[index]);
    }
  }

  public class ScoreNameNotExists extends Exception {

    private final String name;

    public ScoreNameNotExists(String message, String name) {
      super(message);
      this.name = name;
    }
  }
}
