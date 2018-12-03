/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umich.opensearch.kde.gui;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dmitriya
 */
public class KDEGuiProps extends Properties {

  public static final String PROP_FILE_IN = "path.file.in";
  public static final String PROP_FILE_FILTER = "path.file.filter";
  public static final String PROP_FILE_OUT = "path.file.out";
  public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
  public static final String TEMP_FILE_NAME = "opensearchkde.cache";
  private static final long serialVersionUID = -6019756883432498605L;
  private static final Logger log = LoggerFactory.getLogger(KDEGuiProps.class);

  private static Path getTempPath() {
    return Paths.get(TEMP_DIR, TEMP_FILE_NAME);
  }

  public static void deleteTemp() {
    try {
      Files.deleteIfExists(getTempPath());
    } catch (IOException e) {
      log.error("Error while trying to delete stored cache file", e);
    }
  }

  public static KDEGuiProps loadFromTemp() {
    Path path = getTempPath();
    if (!Files.exists(path)) {
      return null;
    }
    try {
      KDEGuiProps props = new KDEGuiProps();
      props.load(new FileInputStream(path.toFile()));
      return props;

    } catch (IOException ex) {
      log.warn("Could not load properties from temporary directory: {}", ex.getMessage());
    }

    return null;
  }

  public static void save(String propName, String propValue) {
    KDEGuiProps props = KDEGuiProps.loadFromTemp();
    props = props == null ? new KDEGuiProps() : props;
    props.put(propName, propValue);
    props.save();
  }

  public void save() {
    Path path = getTempPath();
    try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
      store(fos, "Open Search KDE runtime properties");
    } catch (IOException ex) {
      log.warn("Could not load properties from temporary directory: {}", ex.getMessage());
    }
  }
}
