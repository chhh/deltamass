package umich.opensearch.kde.params;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Use for existing paths only.
 */
public class PathParameter implements IParameterValidator, IStringConverter<Path> {

  @Override
  public void validate(String name, String value) throws ParameterException {
    Path path = Paths.get(value).toAbsolutePath();
    if (Files.notExists(path)) {
      throw new ParameterException(String.format(
          "The specified file doesn't exist: '%s' absolute path: '%s'", value, path.toString()));
    }
    if (!Files.isReadable(path)) {
      throw new ParameterException(String.format(
          "The specified file is not readable: '%s' absolute path: '%s'", value, path.toString()));
    }
  }

  @Override
  public Path convert(String value) {
    return Paths.get(value).toAbsolutePath();
  }
}
