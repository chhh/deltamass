package umich.opensearch.kde.params;

import java.nio.file.Path;

/**
 * @author Dmitry Avtonomov
 */
public class FileChecker {

  String[] exts;

  public FileChecker(String... exts) {
    this.exts = exts;
    for (int i = 0; i < this.exts.length; i++) {
      this.exts[i] = this.exts[i].toLowerCase();
    }
  }

  public boolean accepts(Path path) {
    String pathStr = path.toString().toLowerCase();
    for (String ext : exts) {
      if (pathStr.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }
}
