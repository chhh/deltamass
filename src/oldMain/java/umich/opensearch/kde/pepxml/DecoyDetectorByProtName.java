package umich.opensearch.kde.pepxml;

import java.util.List;
import umich.ms.fileio.filetypes.pepxml.jaxb.standard.SearchHit;

/**
 * @author Dmitry Avtonomov
 */
public class DecoyDetectorByProtName implements DecoyDetector {

  private List<String> protNamePrefixes;
  private String desc;

  public DecoyDetectorByProtName(List<String> protNamePrefixes) {
    this.protNamePrefixes = protNamePrefixes;
    this.desc = String.format("Protein name starts with %s", this.protNamePrefixes);
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public boolean apply(SearchHit searchHit) {
    for (String protNamePrefix : protNamePrefixes) {
      if (searchHit.getProtein().startsWith(protNamePrefix)) {
        return true;
      }
    }
    return false;
  }
}
