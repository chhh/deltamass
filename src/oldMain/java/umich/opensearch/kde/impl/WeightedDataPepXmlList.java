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

package umich.opensearch.kde.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.opensearch.kde.OpenSearchParams;
import umich.opensearch.kde.api.IWeightedData;
import umich.opensearch.kde.api.PepXmlContent;
import umich.opensearch.kde.api.SearchHitDiff;
import umich.opensearch.kde.api.SearchHitDiffs;
import umich.opensearch.kde.params.MassCorrection;
import umich.opensearch.kde.params.WeightFetcherSearchHitDiff;

/**
 * @author Dmitry Avtonomov
 */
public class WeightedDataPepXmlList implements IWeightedData {

  private static final Logger log = LoggerFactory.getLogger(WeightedDataPepXmlList.class);
  private double[] massDiffs;
  private double[] weights;

  private WeightedDataPepXmlList() {
  }

  public static WeightedDataPepXmlList create(List<PepXmlContent> pepXmlList,
      OpenSearchParams params) {
    int totalEntries = countHits(pepXmlList);
    WeightedDataPepXmlList result = new WeightedDataPepXmlList();
    result.massDiffs = new double[totalEntries];
    result.weights = null;

    if (params.getFilterFile() != null) {
      log.info("Loading filter file");
      // TODO: load the file into hashmap

    }

    log.info("Getting data for KDE");
    MassCorrection massCorrection = params.getMassCorrection();
    if (params.isUseWeights()) {
      log.debug("Fetching mass diffs and weights started");
      log.info(String
          .format("Fetching weights, using score '%s' as weight", params.getWeightsScoreName()));
      result.weights = new double[totalEntries];
      WeightFetcherSearchHitDiff.Factory weightFetcherFactory = params.getWeightFetcherFactory();

      int idx = 0;
      for (PepXmlContent pepXmlContent : pepXmlList) {
        List<SearchHitDiffs> hitsList = pepXmlContent.getHitsList();
        for (SearchHitDiffs searchHitDiffs : hitsList) {

          WeightFetcherSearchHitDiff w = weightFetcherFactory
              .create(searchHitDiffs.getMapScoreName2Index());
          List<SearchHitDiff> hits = searchHitDiffs.getHits();
          for (SearchHitDiff hit : hits) {
            switch (massCorrection) {
              case NONE:
                result.massDiffs[idx] = hit.getMassDiff();
                break;
              default:
                result.massDiffs[idx] = hit.getMassDiffCal();
                break;
            }
            result.weights[idx] = w.fetch(hit);
            idx++;
          }
        }
      }
      log.debug("Fetching mass diffs and weights done");
    } else {
      log.debug("Fetching mass diffs without weights started");
      int idx = 0;
      for (PepXmlContent pepXmlContent : pepXmlList) {
        List<SearchHitDiffs> hitsList = pepXmlContent.getHitsList();
        for (SearchHitDiffs searchHitDiffs : hitsList) {
          List<SearchHitDiff> hits = searchHitDiffs.getHits();
          for (SearchHitDiff hit : hits) {
            switch (massCorrection) {
              case NONE:
                result.massDiffs[idx] = hit.getMassDiff();
                break;
              default:
                result.massDiffs[idx] = hit.getMassDiffCal();
                break;
            }
            idx++;
          }
        }
      }
      log.debug("Fetching mass diffs without weights done");
    }
    return result;
  }

  private static int countHits(List<PepXmlContent> list) {
    int count = 0;
    for (PepXmlContent pepXmlContent : list) {
      List<SearchHitDiffs> hitsList = pepXmlContent.getHitsList();
      for (SearchHitDiffs searchHitDiffs : hitsList) {
        List<SearchHitDiff> hits = searchHitDiffs.getHits();
        count += hits.size();
      }
    }
    return count;
  }

  @Override
  public double[] getData() {
    return massDiffs;
  }

  @Override
  public double[] getWeights() {
    return weights;
  }
}
