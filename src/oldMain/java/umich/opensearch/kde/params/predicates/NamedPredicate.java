package umich.opensearch.kde.params.predicates;

import com.google.common.base.Predicate;
import java.io.Serializable;

/**
 * @author Dmitry Avtonomov
 */
public interface NamedPredicate<T> extends Predicate<T>, Serializable {

  /**
   * Provide a short description of how this predicate operates.
   */
  String getDescription();
}
