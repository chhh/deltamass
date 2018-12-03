package com.dmtavt.deltamass.messages;

import java.nio.file.Path;
import java.util.List;

public class MsgCleanup {
  public final Object sender;
  public final List<Path> paths;

  public MsgCleanup(Object sender, List<Path> paths) {
    this.sender = sender;
    this.paths = paths;
  }
}
