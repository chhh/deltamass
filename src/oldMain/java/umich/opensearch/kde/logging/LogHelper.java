package umich.opensearch.kde.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import org.slf4j.LoggerFactory;

/**
 * @author Dmitry Avtonomov
 */
public class LogHelper {

  private LogHelper() {
  }

  /**
   * Configures JUL (java.util.logging) using the logging.properties file located in this package.
   * Only use this method for testing purposes, clients should configure logging themselves - that
   * is you need to provide a logging bridge for SLF4J compatible to your logging infrastructure, or
   * use SLF4J no-op logger.
   *
   * @deprecated Switched to slf4j-simple. The config file 'simplelogger.properties' is in thr
   * 'resources' folder.
   */
  @Deprecated
  public static void configureJavaUtilLogging() {

    try (InputStream is = LogHelper.class.getResourceAsStream("logging.properties")) {
      LogManager logMan = LogManager.getLogManager();
      logMan.readConfiguration(is);
    } catch (final IOException e) {
      java.util.logging.Logger.getAnonymousLogger().severe(
          "Could not load development logging.properties file using "
              + "LogHelper.class.getResourceAsStream(\"/logging.properties\")");
      java.util.logging.Logger.getAnonymousLogger().severe(e.getMessage());
    }

    // print internal state
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        StatusPrinter.print(lc);
  }

  private static Logger createLoggerFor(String string, String file) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder ple = new PatternLayoutEncoder();

    ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
    ple.setContext(lc);
    ple.start();
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
    fileAppender.setFile(file);
    fileAppender.setEncoder(ple);
    fileAppender.setContext(lc);
    fileAppender.start();

    Logger logger = (Logger) LoggerFactory.getLogger(string);
    logger.addAppender(fileAppender);
    logger.setLevel(Level.DEBUG);
    logger.setAdditive(false); /* set to true if root should log too */

    return logger;
  }
}
