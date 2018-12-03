package com.dmtavt.deltamass.logic;

import com.dmtavt.deltamass.DeltaMassInfo;
import com.dmtavt.deltamass.messages.MsgFlushGuiCache;
import com.dmtavt.deltamass.messages.MsgTest;
import com.dmtavt.deltamass.messages.MsgVersionUpdateInfo;
import com.dmtavt.deltamass.ui.FrameSerializer;
import com.dmtavt.deltamass.ui.DeltaMassOptionsForm;
import com.dmtavt.deltamass.ui.DeltaMassUiElements;
import com.github.chhh.utils.SwingUtils;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicGui {

  private static final Logger log = LoggerFactory.getLogger(LogicGui.class);
  private final EventBus bus = EventBus.getDefault();
  private WeakReference<JFrame> frameRef = new WeakReference<>(null);
  private WeakReference<JScrollPane> scrollRef = new WeakReference<>(null);

  CommandGui cmd;


  public LogicGui(CommandGui cmd) {
    this.cmd = cmd;
    bus.register(this);
  }

  public void run() {
    // try restore old cached window
    final JFrame frame;
    frame = new JFrame(DeltaMassInfo.getNameVersion());
    frameRef = new WeakReference<>(frame);
    SwingUtilities.invokeLater(() -> SwingUtils.setUncaughtExceptionHandlerMessageDialog(frame));
    setLaf();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setMainFrameContent(frame);
    frame.setJMenuBar(createMenuBar());
    frame.pack();
    frame.setVisible(true);
    frame.addWindowListener(new FrameSerializer(frame));
    SwingUtils.centerFrame(frame);
    SwingUtils.setFrameIcons(frame, DeltaMassUiElements.ICON_NAMES, DeltaMassUiElements.class);
  }

  private void setMainFrameContent(JFrame frame) {
    JPanel p = new DeltaMassOptionsForm();
    final JScrollPane oldScroll = scrollRef.get();
    if (oldScroll != null) {
      frame.remove(oldScroll);
    }
    final JScrollPane newScroll = new JScrollPane();
    scrollRef = new WeakReference<>(newScroll);
    newScroll.setViewportView(p);
    frame.add(newScroll);
    frame.revalidate();
  }

  @Subscribe
  public void onMsgTest(MsgTest m) {
    log.debug("MsgTest received: " + m.message);
  }

  private JMenuBar createMenuBar() {
    final JMenuBar bar = new JMenuBar();
    final JMenu menuFile = new JMenu("File");
    final JMenuItem close = new JMenuItem("Exit");
    close.addActionListener(e -> {
      Component parent = SwingUtils.findParentFrameForDialog(bar);
      if (parent instanceof JFrame) {
        parent.dispatchEvent(new WindowEvent((Window) parent, WindowEvent.WINDOW_CLOSING));
      }
    });
    menuFile.add(close);

    final JMenu menuAbout = new JMenu("Help");
    final JMenuItem itemFlushGuiCache = new JMenuItem("Restore default parameters");
    itemFlushGuiCache.addActionListener(e -> {
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(bar, "Restore defaults?")) {
        bus.post(new MsgFlushGuiCache(itemFlushGuiCache));
        final JFrame frame = frameRef.get();
        setMainFrameContent(frame);
      }
    });
    menuAbout.add(itemFlushGuiCache);

    bar.add(menuFile);
    bar.add(menuAbout);

    return bar;
  }

  private void setLaf() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException e1) {
      log.info("Could not load native LAF.", e1);
      try {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException e2) {
        log.warn("Could not set Look-And-Feel.");
      }
    }
  }
}
