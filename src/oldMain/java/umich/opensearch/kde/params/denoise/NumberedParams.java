/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umich.opensearch.kde.params.denoise;

import java.util.Properties;

/**
 * @author Dmitry Avtonomov
 */
public abstract class NumberedParams extends Properties {

  public abstract int getNumberOfParameters();

  /**
   * @param i 0-based parameter number
   * @return a valid value of parameter
   */
  public abstract double getParameter(int i);
}
