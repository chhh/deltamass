package umich.opensearch.kde.pepxml;

import umich.ms.fileio.filetypes.pepxml.jaxb.standard.SearchHit;

/**
 * @author Dmitry Avtonomov
 */
public class DecoyDetectorAcceptAll implements DecoyDetector {

  @Override
  public String getDescription() {
    return "All peptides accepted as forwards";
  }

  @Override
  public boolean apply(SearchHit searchHit) {
    return false;
  }
}
