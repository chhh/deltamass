package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;

public class UserOptsPlot {

  private static final int ORDER = 500;
  private static final int EXTRA = ORDER * 1000;

  @Parameter(names = "--mStep", description = "Mass step used for plotting the KDE.",
      order = ORDER + 10)
  public double step = 0.01;

  @Parameter(names = {"--no-peaks"}, description = "No peak detection.",
      order = ORDER + 20)
  public boolean noPeaks = false;

  @Parameter(names = {"--gmm"}, description = "Plot the final Gaussian Mixture Model. Can be slow.",
      order = ORDER + 30)
  public boolean isGmm = false;

  @Parameter(names = {"--exit-on-close"}, hidden = true,
      description = "Stop the program when the plot window is close? Only useful for NOT closing "
          + "the GUI window.")
  public boolean exitOnClose = true;
}
