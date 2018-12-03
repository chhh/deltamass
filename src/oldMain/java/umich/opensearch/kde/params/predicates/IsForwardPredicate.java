package umich.opensearch.kde.params.predicates;

import umich.opensearch.kde.api.SearchHitDiff;

/**
 * @author Dmitry Avtonomov
 */
public class IsForwardPredicate implements NamedPredicate<SearchHitDiff> {

  @Override
  public String getDescription() {
    return "Non-decoy sequences";
  }

  @Override
  public boolean apply(SearchHitDiff searchHitDiff) {
    return !searchHitDiff.isDecoy();
  }
}
