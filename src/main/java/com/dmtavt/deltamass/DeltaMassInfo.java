package com.dmtavt.deltamass;

import com.dmtavt.deltamass.messages.MsgVersionUpdateInfo;
import com.github.chhh.utils.PathUtils;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.VersionComparator;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DeltaMassInfo {
  private static final Logger log = LoggerFactory.getLogger(DeltaMassInfo.class);
  private static final EventBus bus = EventBus.getDefault();

  public static final String Name = "DeltaMass";
  public static final String Ver = "1.0";

  private static final String PROP_VER = "deltamass.version.current";
  private static final String PROP_DOWNLOAD_URL = "deltamass.download.url";
  private static final String DEFAULT_DOWNLOAD_URL = "https://github.com/chhh/deltamass/releases";
  private static final String BUNDLE_PATH = "com/dmtavt/deltamass/Bundle";
  private static final String BUNDLE_URL =
      "https://raw.githubusercontent.com/chhh/deltamass/master/src/main/java/" + BUNDLE_PATH
      + ".properties";
  private static final URI BUNDLE_URI = URI.create(BUNDLE_URL);

  private DeltaMassInfo() {
  }

  public static Path getCacheDir() {
    final Path tempDir = PathUtils.getTempDir();
    return tempDir.resolve(".deltamass");
  }

  public static String getNameVersion() {
    return Name + " (v" + Ver + ")";
  }

  /**
   * Tries to fetch new version info from remote. Runs in a separate thread, posts
   * a sticky message to the default {@link EventBus} about a new version in case
   * of success.
   */
  public static void checkForNewVersions() {
    Thread t = new Thread(() -> {
      try {

        log.debug("Checking for new versions");
        final Charset utf8 = Charset.forName("UTF-8");
        final Properties props = new Properties();

        // read from remote
        final URL url = BUNDLE_URI.toURL();
        log.debug("Fetching update info, trying to get file: {}", url.toString());
        final String remoteFileContent = IOUtils.toString(url, utf8);
        if (StringUtils.isBlank(remoteFileContent)) {
          log.debug("Remote version file fetched, but was empty");
          return;
        }
        log.debug("Remote version file fetched");
        props.load(new StringReader(remoteFileContent));

        // this is used to test functionality without pushing changes to github
//        props.put(PROP_VER, "5.7");

        // add new versions notification

        final VersionComparator vc = new VersionComparator();
        final String remoteVer = props.getProperty(PROP_VER);
        log.debug("Remote version: {}, local verison: {}", remoteVer, Ver);
        if (remoteVer != null) {
          final int compare = vc.compare(Ver, remoteVer);
          log.debug("Version comparison result: {}" , compare);
          if (compare < 0) {
            log.debug("Posting notification about a newer version updateg");
            String downloadUrl = props.getProperty(PROP_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL);
            bus.postSticky(new MsgVersionUpdateInfo(Name, Ver, remoteVer, downloadUrl));
          }
        } else {
          log.debug("Could not parse remote version");
          return;
        }


      } catch (Exception e) {
        // it doesn't matter, it's fine if we can't fetch the file from github
        log.info("Could not get version update info from GitHub");
      }
    });
    t.start();
  }

}
