package umich.opensearch.kde.params;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Dmitry Avtonomov
 */
public class WriteableDirectoryParameter implements IParameterValidator, IStringConverter<Path>,
    IValueValidator<Path> {

  @Override
  public void validate(String name, String value) throws ParameterException {
    try {
      Path path = Paths.get(value);
    } catch (InvalidPathException e) {
      throw new ParameterException(e);
    }
  }

  @Override
  public Path convert(String value) {
    return Paths.get(value).toAbsolutePath();
  }

  @Override
  public void validate(String name, Path path) throws ParameterException {
    if (Files.notExists(path)) {
      throw new ParameterException(String.format(
          "The specified directory doesn't exist (%s).", path));
    }
    if (!Files.isDirectory(path)) {
      throw new ParameterException(String.format(
          "Provided path must be an existing directory, not a file (%s).", path));
    }
    if (!Files.isWritable(path)) {
      throw new ParameterException(String.format(
          "The specified directory is not writable (%s)", path));
    }
  }
}
