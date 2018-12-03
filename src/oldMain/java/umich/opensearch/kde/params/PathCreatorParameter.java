package umich.opensearch.kde.params;

import com.beust.jcommander.IStringConverter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Use for paths that are to be created.
 */
public class PathCreatorParameter implements IStringConverter<Path> {

  @Override
  public Path convert(String value) {
    return Paths.get(value).toAbsolutePath();
  }
}
