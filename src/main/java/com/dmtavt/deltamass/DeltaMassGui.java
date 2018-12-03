package com.dmtavt.deltamass;

import com.dmtavt.deltamass.logging.LogbackJTextPaneAppender;
import com.dmtavt.deltamass.ui.DeltaMassUiElements;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.swing.TextConsole;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Paint;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaMassGui extends JFrame {
  private static final Logger log = LoggerFactory.getLogger(DeltaMassGui.class);
  private LogbackJTextPaneAppender appender;
  public static final List<String> ICON_NAMES = Arrays.asList(
      "delta_logo_16.png", "delta_logo_32.png", "delta_logo_48.png",
      "delta_logo_64.png", "delta_logo_96.png", "delta_logo_128.png", "delta_logo_256.png");
  private ExecutorService exec;
  private TextConsole console;
  private JScrollPane scrollConsole;

  public static final Paint[] COLORS_BETTER = new Paint[] {
      new Color(106,61,154),
      new Color(255,127,0),
      new Color(227,26,28),
      new Color(51,160,44),
      new Color(31,120,180),
      new Color(202,178,214),
      new Color(253,191,111),
      new Color(251,154,153),
      new Color(178,223,138),
      new Color(166,206,227),
  };

  public DeltaMassGui() throws HeadlessException {
    super();
    initComponents();
    initBehaviors();
    initLaf();
  }

  private void initComponents() {

  }

  private void initBehaviors() {
    setTitle(DeltaMassInfo.getNameVersion());
    exec = Executors.newCachedThreadPool();
    SwingUtils.setFrameIcons(this, ICON_NAMES, DeltaMassUiElements.class);

    console = new TextConsole();
    scrollConsole.setViewportView(console);

    appender = new LogbackJTextPaneAppender();
    appender.start();
    log.debug("Started LogbackJTextPaneAppender logger");
    appender.setTextPane(console);


  }

  private void initLaf() {

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      try {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
        log.warn("Neither System nor Nimbus look-and-feel found.");
      } catch (Exception e1) {
        log.warn("Could not initialize Look-And-Feel (LAF).");
      }
    }
  }
}
