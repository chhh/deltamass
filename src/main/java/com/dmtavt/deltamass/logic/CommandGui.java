package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Runs the GUI where you can specify all the options.",
    commandNames = {CommandGui.CMD})
public class CommandGui {

  public static final String CMD = "gui";

  @Parameter(names = {"-h",
      "--help"}, hidden = true, help = true, description = "Display help message.")
  boolean help = false;
}
