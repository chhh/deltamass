package com.dmtavt.deltamass.ui;

import com.dmtavt.deltamass.DeltaMassInfo;
import com.dmtavt.deltamass.logic.LogicGui;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.swing.JFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameSerializer extends WindowAdapter {
  private static final Logger log = LoggerFactory.getLogger(FrameSerializer.class);
  private final JFrame frame;

  public FrameSerializer(JFrame frame) {
    this.frame = frame;
  }

//  @Override
//  public void windowClosing(WindowEvent e) {
//    super.windowClosing(e);
//
//    // try save the window state
//    final Path cacheDir = DeltaMassInfo.getCacheDir();
//
//    if (!Files.exists(cacheDir)) {
//      try {
//        Files.createDirectories(cacheDir);
//      } catch (IOException e1) {
//        log.error("Could not create directory structure for caching.", e1);
//        return;
//      }
//    }
//
//    final Path cachePath = cacheDir.resolve(LogicGui.getSerializedFilename());
//    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cachePath,
//        StandardOpenOption.CREATE))) {
//      oos.writeObject(frame.getContentPane());
//    } catch (IOException e1) {
//      log.error("Could not serialize form.");
//    }
//  }
}
