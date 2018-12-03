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

package umich.opensearch.kde;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * @author Dmitry Avtonomov
 */
public class BinReader {

  public static double[][] readData(Path path, boolean isBinary, OpenSearchParams params) {
    boolean dataHasWeights = params.getColNumWeight() != null;
    if (!dataHasWeights) {
      if (isBinary) {
        int numDataPoints = (int) (path.toFile().length() / 8);
        double[] result = new double[numDataPoints];
        try (DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(path.toFile())))) {
          double val;
          int i = 0;
          while (true) {
            val = dis.readDouble();
            result[i] = val;
            i++;
          }
        } catch (EOFException e) {
          // simply reached the end of file, all fine
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new double[][]{result};
      } else {
        final int colNumData = params.getColNumData();
        ArrayList<Double> listVals = new ArrayList<>((int) 1e6);
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
          double val;
          String line;
          String[] split;
          Pattern delimiterRegEx = Pattern.compile(params.getDelimiterRegEx());
          if (params.isHeaderRow()) {
            reader.readLine();
          }
          while ((line = reader.readLine()) != null) {
            split = delimiterRegEx.split(line);
            val = Double.parseDouble(split[colNumData]);
            listVals.add(val);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        double[] result = new double[listVals.size()];
        for (int i = 0; i < listVals.size(); i++) {
          result[i] = listVals.get(i);
        }
        return new double[][]{result};
      }
    } else {
      if (isBinary) {
        int numDataPoints = (int) ((path.toFile().length() / 8) / 2);
        double[] result = new double[numDataPoints];
        double[] weights = new double[numDataPoints];
        try (DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(path.toFile())))) {
          double val;
          int i = 0;
          while (true) {
            val = dis.readDouble();
            result[i] = val;
            val = dis.readDouble();
            weights[i] = val;
            i++;
          }
        } catch (EOFException e) {
          // simply reached the end of file, all fine
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new double[][]{result};
      } else {
        ArrayList<Double> listVals = new ArrayList<>((int) 1e6);
        ArrayList<Double> listWeights = new ArrayList<>((int) 1e6);
        final int colNumData = params.colNumData;
        final int colNumWeight = params.colNumWeight;
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
          double val;
          String line;
          String[] split;
          Pattern delimiterRegEx = Pattern.compile(params.getDelimiterRegEx());
          if (params.isHeaderRow()) {
            reader.readLine();
          }
          while ((line = reader.readLine()) != null) {
            split = delimiterRegEx.split(line);
            val = Double.parseDouble(split[colNumData]);
            listVals.add(val);
            val = Double.parseDouble(split[colNumWeight]);
            listWeights.add(val);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (listVals.size() != listWeights.size()) {
          throw new IllegalStateException("Resulting lists must be of the same size");
        }
        double[] vals = new double[listVals.size()];
        double[] weights = new double[listWeights.size()];
        for (int i = 0; i < listWeights.size(); i++) {
          vals[i] = listVals.get(i);
          weights[i] = listWeights.get(i);
        }
        return new double[][]{vals, weights};
      }
    }
  }
}
