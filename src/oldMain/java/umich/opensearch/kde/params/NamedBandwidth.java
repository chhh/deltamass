package umich.opensearch.kde.params;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

/**
 * @author Dmitry Avtonomov
 */
public class NamedBandwidth {

  public final String name;
  public final double bandwidth;
  public final double targetMz;
  public final double window;

  /**
   * Bandwidth with label
   *
   * @param name to be used on the plot
   * @param bandwidth use NaN or Infinity to indicate that you want dynamic bandwidth estimation
   */
  public NamedBandwidth(String name, double bandwidth, double targetMz, double window) {
    this.name = name;
    this.bandwidth = bandwidth;
    this.window = window;
    this.targetMz = targetMz;
  }

  public NamedBandwidth(String name, double bandwidth) {
    this.name = name;
    this.bandwidth = bandwidth;
    this.window = Double.NaN;
    this.targetMz = Double.NaN;
  }

  public String getName() {
    return name;
  }

  public double getBandwidth() {
    return bandwidth;
  }

  public double getTargetMz() {
    return targetMz;
  }

  public double getWindow() {
    return window;
  }

  public boolean isBandwidthAutoAtFixedMz() {
    return !Double.isNaN(window) && !Double.isNaN(targetMz);
  }

  public boolean isBandwidthAuto() {
    return !Double.isNaN(window) && Double.isNaN(targetMz);
  }

  public String getFilenameAddon() {
    StringBuilder sb = new StringBuilder();
    DecimalFormat f = new DecimalFormat("0.###########");
    if (isBandwidthAuto()) {
      sb.append("-h-auto");
    } else if (isBandwidthAutoAtFixedMz()) {
      sb.append("-h-auto-at-").append(f.format(targetMz)).append("-").append(f.format(window));
    } else {
      sb.append("-h-").append(f.format(bandwidth));
    }
    return sb.toString();
  }

  /**
   * Propose a name for the output file of KDE peak picking results.
   *
   * @param in should be a directory path, at laest the file will be created relative to this path
   * @return full file path of the output file
   */
  public Path createOutputPath(Path in) {
    return Paths.get(in.toAbsolutePath().toString(), "KDE" + getFilenameAddon() + ".tsv");
  }
}
