package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.dmtavt.deltamass.args.DecoyTreatment;
import com.dmtavt.deltamass.args.Kernel;

public class UserOptsKde {

  private static final int ORDER = 300;
  private static final int EXTRA = ORDER * 1000;

  @Parameter(names = {"-b", "--bandwidth"}, description =
      "KDE bandwidth. If less or equal to zero - automatic " +
          "determination at each nominal mass.", order = ORDER + 10)
  public double bandwidth = 0.0;

  @Parameter(names = {"-k", "--kernel"}, description = "Kernel type. GAUSS should be " +
      "good enough for most cases. Use EPANECHNIKOV if you have huge datasets and KDE " +
      "takes too long.", hidden = true, order = EXTRA + 20)
  public Kernel.Type kernel = Kernel.Type.GAUSS;

  @Parameter(names = {"--dx", "--step"}, description = "Granularity of X axis.")
  public double step = 0.01;

  @Parameter(names = {"--min-data"}, description = "Min number of data points (PSMs) to "
      + "consider a nominal mass region for KDE.", order = ORDER + 40)
  public int minData = 10;
}
