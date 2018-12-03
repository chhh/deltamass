package umich.opensearch.kde.params;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Dmitry Avtonomov
 */
public class CSVFormatValidator implements IValueValidator<String> {

  @Override
  public void validate(String s, String val) throws ParameterException {
    List<String> csvFormats = Arrays.asList("csv", "tsv", "excel");
    if (!csvFormats.contains(val)) {
      throw new ParameterException(String.format("Supported formats for \"%s\" optiion are: %s", s,
          Arrays.toString(csvFormats.toArray())));
    }
  }
}
