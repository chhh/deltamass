package umich.opensearch.kde.params;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.CommaParameterSplitter;
import java.util.List;

/**
 * @author Dmitry Avtonomov
 */
public class PathListParameter implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {

    CommaParameterSplitter splitter = new CommaParameterSplitter();
    List<String> splittedValues = splitter.split(value);

    PathParameter pathParameter = new PathParameter();
    for (String val : splittedValues) {
      pathParameter.validate(name, val);
    }
  }
}
