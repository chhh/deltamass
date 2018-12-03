/*
 * Copyright (c) 2017 Dmitry Avtonomov
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

package umich.opensearch.kde.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for working with delimited files.
 *
 * @author Dmitry Avtonomov
 */
public class DelimitedFileUtil {

  private DelimitedFileUtil() {
  }

  /**
   * Tries to guess a single character delimiter from several lines of input.<br/> Only guesses
   * between comma, tab, semicolon and space (<b>multiple spaces not supported!</b>).
   *
   * @param lines E.g. lines of a delimited text file.
   * @return Null in case a delimiter could not be guessed.
   */
  public Character guessDelimiter(List<String> lines) {
    char[] delimiters = {' ', '\t', ',', ';'};
    int[][] counts = new int[lines.size()][delimiters.length]; // [line][delimiter]
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      for (int j = 0; j < line.length(); j++) {
        for (int k = 0; k < delimiters.length; k++) {
          if (line.charAt(j) == delimiters[k]) {
            counts[i][k]++;
          }
        }
      }
    }

    Map<Character, Integer> map = new HashMap<>();
    for (int i = 0; i < delimiters.length; i++) {
      char delimiter = delimiters[i];
      boolean isSameCounts = true;
      int cntPrev = counts[0][i];
      for (int j = 0; j < lines.size(); j++) {
        if (counts[j][i] != cntPrev) {
          isSameCounts = false;
          break;
        }
        cntPrev = counts[j][i];
      }
      if (isSameCounts) {
        map.put(delimiter, cntPrev);
      }
    }

    Character delimiter = null;
    Integer count = -1;
    for (Map.Entry<Character, Integer> entry : map.entrySet()) {
      if (entry.getValue() > count) {
        count = entry.getValue();
        delimiter = entry.getKey();
      }
    }

    return count <= 0 ? null : delimiter;
  }

  /**
   * Reads N lines from a stream.
   *
   * @param is The stream to read from. Will be wrapped into a BufferedReader using UTF-8 charset.
   * @param n The number of lines to read.
   * @param nonEmpty If true, empty lines will be skipped. Lines containing whitespace only will NOT
   * be skipped as whitespace characters can be delimiters.
   * @return List of read lines. The list might contain fewer lines than requested if there were
   * fewer lines in the input.
   */
  public List<String> readLines(InputStream is, int n, boolean nonEmpty) throws IOException {
    List<String> lines = new ArrayList<>(n);
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(is, Charset.forName("UTF-8")))) {
      for (int i = 0; i < n; i++) {
        String line = br.readLine();
        if (nonEmpty && line.isEmpty()) {
          continue;
        }
        lines.add(line);
      }
    }
    return lines;
  }
}
