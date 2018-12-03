package umich.opensearch.kde.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * @author Dmitry Avtonomov
 */
public class DecimalFormatWithToString extends DecimalFormat {

  public DecimalFormatWithToString() {
  }

  public DecimalFormatWithToString(String pattern) {
    super(pattern);
  }

  public DecimalFormatWithToString(String pattern, DecimalFormatSymbols symbols) {
    super(pattern, symbols);
  }

  @Override
  public String toString() {
    return this.toPattern();
  }
}
