package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

@Parameters(commandDescription = "Do kernel density estimation (KDE) and detect peaks, " +
    "write peaks to a file.", commandNames = {CommandPeaks.CMD})
public class CommandPeaks {

  public static final String CMD = "peaks";

  @Parameter(names = {"-h", "--help"},
      hidden = true, help = true, description = "Display help message.")
  boolean help = false;

  @ParametersDelegate
  public UserOptsInputFiles optsInputFiles = new UserOptsInputFiles();

  @ParametersDelegate
  public UserOptsKde optsKde = new UserOptsKde();

  @ParametersDelegate
  public UserOptsPeaks optsPeaks = new UserOptsPeaks();
}
