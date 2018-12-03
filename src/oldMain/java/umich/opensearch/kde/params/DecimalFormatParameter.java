package umich.opensearch.kde.params;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import java.text.DecimalFormat;

/**
 * @author Dmitry Avtonomov
 */
public class DecimalFormatParameter implements IParameterValidator, IStringConverter<DecimalFormat>,
    IValueValidator<DecimalFormat> {

  @Override
  public void validate(String name, String value) throws ParameterException {
    DecimalFormat format = new DecimalFormat();
    try {
      format.applyPattern(value);
    } catch (IllegalArgumentException e) {
      throw new ParameterException(String.format(
          "Parameter '%s' had incorrect format '%s'. " +
              "Check https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html " +
              "for correct formats.", name, value));
    }
  }

  @Override
  public DecimalFormat convert(String value) {
    return new DecimalFormat(value);
  }

  @Override
  public void validate(String name, DecimalFormat value) throws ParameterException {

  }
}
