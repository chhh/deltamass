package umich.opensearch.kde.params.predicates;

import umich.opensearch.kde.api.SearchHitDiff;

/**
 * @author Dmitry Avtonomov
 */
public class IsDecoyPredicate implements NamedPredicate<SearchHitDiff> {

  @Override
  public boolean apply(SearchHitDiff searchHitDiff) {
    return searchHitDiff.isDecoy();
  }

  @Override
  public String getDescription() {
    return "Decoy sequences";
  }
}
