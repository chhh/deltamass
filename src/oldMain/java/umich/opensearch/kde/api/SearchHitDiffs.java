/*
 * Copyright (c) 2016 Dmitry Avtonomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package umich.opensearch.kde.api;

import com.github.chhh.utils.exceptions.ParsingException;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.opensearch.kde.params.MassCorrection;
import umich.opensearch.kde.params.predicates.ScorePredicate;

/**
 * @author Dmitry Avtonomov
 */
public class SearchHitDiffs implements Serializable {

  public static final Logger log = LoggerFactory.getLogger(SearchHitDiffs.class);
  private static final long serialVersionUID = 47769708261008276L;
  private List<SearchHitDiff> hits;
  private MassCorrection massCorrection;
  private volatile boolean isSorted = false;
  private Map<String, Integer> mapScoreName2Index = new HashMap<>(10);
  private String name = "";

  public SearchHitDiffs(List<SearchHitDiff> hits, Map<String, Integer> scoreMap,
      MassCorrection massCorrection) {
    this.hits = hits;
    this.mapScoreName2Index = scoreMap;
    this.massCorrection = massCorrection;
  }

  public static double[] getMassDiffsArray(List<SearchHitDiff> shds) {
    double[] diffs = new double[shds.size()];
    for (int i = 0; i < diffs.length; i++) {
      diffs[i] = shds.get(i).getMassDiff();
    }
    return diffs;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MassCorrection getMassCorrection() {
    return massCorrection;
  }

  public void setMassCorrection(MassCorrection massCorrection) {
    this.massCorrection = massCorrection;
  }

  public Map<String, Integer> getMapScoreName2Index() {
    return mapScoreName2Index;
  }

  public boolean isSorted() {
    return isSorted;
  }

  public List<SearchHitDiff> getHits() {
    return hits;
  }

  public void sortByDiffMass() {
    hits.sort(Comparator.comparingDouble(SearchHitDiff::getMassDiffCal));
    isSorted = true;
  }

  public double[] getMassDiffsArray() {
    double[] diffs = new double[hits.size()];
    for (int i = 0; i < hits.size(); i++) {
      diffs[i] = hits.get(i).getMassDiff();
    }
    return diffs;
  }

  /**
   * @param weightIdx the index of the score from pep.xml file to be used as a weight
   */
  public double[] getWeightsArray(int weightIdx) {
    double[] weights = new double[hits.size()];
    for (int i = 0; i < hits.size(); i++) {
      weights[i] = hits.get(i).getScores()[weightIdx];
    }
    return weights;
  }

  public void printPepsByMass(double mzLo, double mzHi, int maxCountToPrint) {
    printPepsByMass(System.out, mzLo, mzHi, maxCountToPrint);
  }

  public void printPepsByMass(Appendable out, double mzLo, double mzHi, int maxCountToPrint) {
    try {
      if (!isSorted) {
        throw new IllegalStateException(
            "You need to call .sortByDiffMass() before calling this method.");
      }
      SearchHitDiff shd = new SearchHitDiff(1);

      out.append(String.format("Peptides in mass diff range [%.4f; %.4f]:\n", mzLo, mzHi));

      shd.setMassDiff(mzLo);

      int idxLo = findPepIndex(shd);
      if (idxLo < 0) {
        idxLo = -idxLo - 1;
      }

      shd.setMassDiff(mzHi);
      int idxHi = findPepIndex(shd);
      if (idxHi < 0) {
        idxHi = -idxHi - 1;
      }
      List<SearchHitDiff> found = hits.subList(idxLo, idxHi);
      HashMap<String, Integer> map = new HashMap<>();
      for (SearchHitDiff searchHitDiff : found) {
        map.merge(searchHitDiff.getSeq(), 1, (a, b) -> a + b);
      }
      int countPrinted = 0;
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        out.append(String.format("\t%s : %d hits\n", entry.getKey(), entry.getValue()));
        if (++countPrinted >= maxCountToPrint) {
          break;
        }
      }
      if (map.size() > maxCountToPrint) {
        out.append("\tPeptide list too long, not printing all peptides\n");
      }
      out.append(String
          .format("\tTotal %d distinct sequences, %d overall hits\n", map.size(), found.size()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private int findPepIndex(SearchHitDiff shd) {
    return Collections
        .binarySearch(hits, shd, Comparator.comparingDouble(SearchHitDiff::getMassDiff));
  }

  public List<SearchHitDiff> findDiffsByMassDiffRange(double mLo, double mHi) {
    Comparator<SearchHitDiff> comparator = Comparator.comparingDouble(SearchHitDiff::getMassDiff);
    SearchHitDiff lo = new SearchHitDiff(0);
    lo.setMassDiff(mLo);
    int idxLo = Collections.binarySearch(hits, lo, comparator);
    SearchHitDiff hi = new SearchHitDiff(0);
    hi.setMassDiff(mHi);
    int idxHi = Collections.binarySearch(hits, hi, comparator);

    if (idxLo < 0) {
      idxLo = -idxLo - 1;
    }

    if (idxHi < 0) {
      idxHi = -idxHi - 1;
    }

    return hits.subList(idxLo, idxHi);
  }


  /**
   * Remove entries that don't fit criteria.
   *
   * @return number of entries removed
   */
  public int filter(Predicate<SearchHitDiff> criteria) {
    return filter(criteria, new ScorePredicate.Factory[]{});
  }

  /**
   * Remove entries that don't fit criteria.
   *
   * @param criteria will be AND'ed with whatever predicates are produced by {@code
   * scorePredicateFactories}
   * @param scorePredicateFactories this has to be passed in separately, because ScorePredicates
   * need to be configured before being used, and that factory configures the predicate.
   * @return number of entries removed
   */
  public int filter(Predicate<SearchHitDiff> criteria,
      ScorePredicate.Factory... scorePredicateFactories) {
    // generate criteria
    if (scorePredicateFactories != null && scorePredicateFactories.length > 0) {
      List<Predicate<SearchHitDiff>> predicates = new ArrayList<>();
      predicates.add(criteria);
      for (ScorePredicate.Factory factory : scorePredicateFactories) {
        log.debug("Creating predicate '{}'", factory.toString());
        try {
          ScorePredicate predicate = factory.create(mapScoreName2Index);
          predicates.add(predicate);
        } catch (ParsingException e) {
          String msg = "Could not configure score predicate, probably the file didn't have that score in the first place, skipping that filter.";
          log.error(msg, e);
          throw new RuntimeException(e);
          //LogUtils.println(out, msg + "\n" + e.getMessage());
        }
      }
      criteria = Predicates.and(predicates);
    }

    // find the array size
    int toBeLeft = 0, startingSize = hits.size();
    List<SearchHitDiff> hitsList = getHits();
    for (SearchHitDiff hit : hitsList) {
      if (criteria.apply(hit)) {
        toBeLeft++;
      }
    }
    if (toBeLeft == startingSize) {
      return 0; // all good, nothing to do
    }

    SearchHitDiff[] filtered = new SearchHitDiff[toBeLeft];
    int i = 0;
    for (SearchHitDiff hit : hitsList) {
      if (criteria.apply(hit)) {
        filtered[i++] = hit;
      }
    }
    this.hits = Arrays.asList(filtered);
    return startingSize - filtered.length;
  }
}
