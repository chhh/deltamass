package umich.opensearch.kde;

import com.beust.jcommander.Parameter;

/**
 * @author Dmitry Avtonomov
 */
public class KdeBinParams extends OpenSearchParams {

  public static String APP_NAME = "KDE Binary Version";
  @Parameter(names = {"--colData"},
      description = "If parsing a delimited file, this column will be used as the data input.")
  public Integer colNumData = 0;
  @Parameter(names = {"--colWeight"},
      description = "If parsing a delimited file, this column can optionally be used as weights.")
  public Integer colNumWeight = null;
  @Parameter(names = {"--hasWeights"},
      description = "Whether the file has weights for data points. This parameter is only needed for binary files.")
  protected boolean hasWeights = false;
  @Parameter(names = {"--delimiterRegEx"},
      description = "Each line of text input will be split according to this regular expression.")
  protected String delimiterRegEx = "\\s";
  @Parameter(names = {"--hasHeader"},
      description = "When parsing a text (delimited) file, if set to true will skip the first row.")
  protected boolean hasHeaderRow = false;

  public boolean hasWeights() {
    return hasWeights;
  }

  public String getDelimiterRegEx() {
    return delimiterRegEx;
  }

  public void setDelimiterRegEx(String delimiterRegEx) {
    this.delimiterRegEx = delimiterRegEx;
  }

  public boolean hasHeaderRow() {
    return hasHeaderRow;
  }

  public void setHasHeaderRow(boolean hasHeaderRow) {
    this.hasHeaderRow = hasHeaderRow;
  }

  public boolean isHasWeights() {
    return hasWeights;
  }

  public void setHasWeights(boolean hasWeights) {
    this.hasWeights = hasWeights;
  }

}
