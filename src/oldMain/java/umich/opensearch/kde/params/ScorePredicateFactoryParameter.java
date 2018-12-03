package umich.opensearch.kde.params;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import umich.opensearch.kde.params.predicates.ComparisonType;
import umich.opensearch.kde.params.predicates.ScorePredicate;


/**
 * @author Dmitry Avtonomov
 */
public class ScorePredicateFactoryParameter implements IParameterValidator,
    IStringConverter<ScorePredicate.Factory> {

  @Override
  public void validate(String name, String value) throws ParameterException {
    try {
      ScorePredicate.Factory predicate = convert(value);
      if (predicate == null) {
        throw new ParameterException(String.format(
            "Invalid score predicate parameter given (%s). Don't use spaces and use '==' for equality.",
            value));
      }
    } catch (ParameterException e) {
      throw new ParameterException(String.format(
          "Invalid score predicate parameter given (%s). Don't use spaces and use '==' for equality.",
          value), e);
    }
  }

  @Override
  public ScorePredicate.Factory convert(String value) {

    ComparisonType[] comparisonTypes = ComparisonType.values();
    for (ComparisonType comparisonType : comparisonTypes) {
      String[] split = value.split(comparisonType.symbol);
      if (split.length == 2) {
        String scoreName = split[0];
        if (scoreName.length() == 0) {
          throw new ParameterException("Score name must be non-zero length");
        }
        String scoreValStr = split[1];
        if (scoreValStr.length() == 0) {
          throw new ParameterException("Score value must be non-zero length");
        }
        try {
          double scoreVal = Double.parseDouble(scoreValStr);
          return new ScorePredicate.Factory(comparisonType, scoreName, scoreVal);
        } catch (NumberFormatException e) {
        }
      }
    }
    return null;
  }
}
