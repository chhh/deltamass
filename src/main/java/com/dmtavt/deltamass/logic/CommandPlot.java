package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

@Parameters(commandDescription = "Do kernel density estimation (KDE) and plot it, " +
    "optionally detect peaks as well.", commandNames = {CommandPlot.CMD})
public class CommandPlot {

  public static final String CMD = "plot";

  @Parameter(names = {"-h", "--help"}, hidden = true,
      help = true, description = "Display help message.")
  boolean help = false;

  @ParametersDelegate
  public UserOptsInputFiles optsInputFiles = new UserOptsInputFiles();

  @ParametersDelegate
  public UserOptsKde optsKde = new UserOptsKde();

  @ParametersDelegate
  public UserOptsPlot optsPlot = new UserOptsPlot();

  @ParametersDelegate
  public UserOptsPeaks optsPeaks = new UserOptsPeaks();

}
