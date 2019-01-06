package com.dmtavt.deltamass.ui;

import com.dmtavt.deltamass.DeltaMassInfo;
import com.dmtavt.deltamass.args.DecoyTreatment;
import com.dmtavt.deltamass.args.MassCorrection;
import com.dmtavt.deltamass.logging.LogbackJTextPaneAppender;
import com.dmtavt.deltamass.logic.CommandClean;
import com.dmtavt.deltamass.logic.CommandPlot;
import com.dmtavt.deltamass.logic.LogicClean;
import com.dmtavt.deltamass.logic.LogicPeaks;
import com.dmtavt.deltamass.logic.LogicPlot;
import com.dmtavt.deltamass.logic.UserOptsInputFiles;
import com.dmtavt.deltamass.logic.UserOptsKde;
import com.dmtavt.deltamass.logic.UserOptsPeaks;
import com.dmtavt.deltamass.logic.UserOptsPlot;
import com.dmtavt.deltamass.messages.MsgCleanup;
import com.dmtavt.deltamass.messages.MsgFlushGuiCache;
import com.dmtavt.deltamass.messages.MsgRunPlot;
import com.dmtavt.deltamass.messages.MsgStop;
import com.dmtavt.deltamass.messages.MsgVersionUpdateInfo;
import com.dmtavt.deltamass.utils.OsUtils;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.ser.SwingCachePropsStore;
import com.github.chhh.utils.swing.DocumentFilters;
import com.github.chhh.utils.swing.FileNameEndingFilter;
import com.github.chhh.utils.swing.TextConsole;
import com.github.chhh.utils.swing.UiCheck;
import com.github.chhh.utils.swing.UiCombo;
import com.github.chhh.utils.swing.UiSpinnerDouble;
import com.github.chhh.utils.swing.UiSpinnerInt;
import com.github.chhh.utils.swing.UiText;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.RoundedBalloonStyle;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaMassOptionsForm extends JPanel {

  private static final Logger log = LoggerFactory.getLogger(DeltaMassOptionsForm.class);
  private static final String DELTAMASS_FORM_CACHE = "deltamass-form.cache";
  private LogbackJTextPaneAppender appender = new LogbackJTextPaneAppender();
  private EventBus bus = EventBus.getDefault();
  private final SwingCachePropsStore cache;
  private BalloonTipStyle style = new RoundedBalloonStyle(5, 5, Color.WHITE, Color.BLACK);
  private BalloonRegistry balloonRegistry = new BalloonRegistry();

  private UiText uiTextInputPaths = new UiText();
  private JButton uiBtnCleanup = new JButton("Cleanup");
  private UiText uiTextReFiles = new UiText();
  private UiText uiTextReDecoys = new UiText("rev_");
  //private JTextField text = new JTextField();
  private UiCheck uiCheckNoCache = new UiCheck();
  private UiCombo uiComboDecoysUse = new UiCombo();
  private UiCombo uiComboMassCorrection = new UiCombo();
  private UiText uiTextAdditionalSearchPaths = new UiText();
  private static DecimalFormat df1 = new DecimalFormat("0.0");
  private static DecimalFormat df2 = new DecimalFormat("0.00");
  private static DecimalFormat df3 = new DecimalFormat("0.000");
  private static DecimalFormat df4 = new DecimalFormat("0.0000");
  private UiCheck uiCheckRangeLimitDo = new UiCheck(null, null, false);
  private UiSpinnerDouble uiSpinnerRangeLimitLo = new UiSpinnerDouble(+5.5, -50000.0, 50000.0, 5.0, df1);
  private UiSpinnerDouble uiSpinnerRangeLimitHi = new UiSpinnerDouble(+85.5, -50000.0, 50000.0, 5.0, df1);
  private UiText uiTextRangeExclusions = new UiText("-0.5 0.5,");
  private UiSpinnerDouble uiSpinnerKdeBandwidth = new UiSpinnerDouble(0.0, 0.0, 0.1, 0.001, 3, df3);
  private UiSpinnerDouble uiSpinnerKdeDx = new UiSpinnerDouble(+0.002, 0.0001, 1.0, 0.0001, 4, df4);
  private UiSpinnerInt uiSpinnerKdeMinData = new UiSpinnerInt(10, 0, Integer.MAX_VALUE, 10);
  private UiCheck uiCheckPeaksDo = new UiCheck(null, null, true);
  private UiSpinnerInt uiSpinnerMinPsm = new UiSpinnerInt(10, 0, Integer.MAX_VALUE, 10);
  private UiSpinnerDouble uiSpinnerMinPct = new UiSpinnerDouble(0.025, 0, 1, 0.005, 3, df3);
  private UiSpinnerDouble uiSpinnerPlotStep = new UiSpinnerDouble(0.004, 0.0001, 0.5, 0.0001, 4, df4);
  private UiText uiTextOutputFile = new UiText();
  private JButton uiBtnRun = new JButton("Run");
  private JButton uiBtnStop = new JButton("Stop");
  private JButton uiBtnConsoleClear = new JButton("Clear Console");

  private ExecutorService exec = Executors.newSingleThreadExecutor();
  private ConcurrentLinkedQueue<Future<?>> tasks = new ConcurrentLinkedQueue<>();
  private ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<>();


  private JPanel pInputs = new JPanel();
  private JPanel pKde = new JPanel();
  private JPanel pOut = new JPanel();
  private JPanel pButtons = new JPanel();
  private JScrollPane scrollConsole = new JScrollPane();
  private TextConsole console = new TextConsole();

  private static final String pathsSeparator = "; ";

  public DeltaMassOptionsForm() {
    super();
    cache = new SwingCachePropsStore(DELTAMASS_FORM_CACHE, DeltaMassInfo::getCacheDir, DeltaMassInfo::getNameVersion);
    init();
  }

  private void init() {
    this.setLayout(new MigLayout(new LC().flowY().fillX()));
    LC lc = new LC();
//    LC lc = new LC().debug();

    pInputs.setLayout(new MigLayout(lc));
    pInputs.setBorder(new TitledBorder("Inputs files"));

    CC ccLbl = new CC().alignX("right").gapBefore("5px");
    CC ccComp = new CC().width("50:70:100px");
    CC ccCompWrap = new CC().width("30:50:70px").wrap();

    // adding components to the inputs panel
    JFileChooser fci = new JFileChooser();
    fci.setAcceptAllFileFilterUsed(true);
    fci.setMultiSelectionEnabled(true);
    fci.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fci.setApproveButtonText("Select");
    fci.addChoosableFileFilter(new FileNameEndingFilter("PepXml", "pepxml", "pep.xml"));
//    fc.addChoosableFileFilter(new FileNameEndingFilter("MzIdentMl", "mzid", "mzidentml", "mzidml"));
    final String textInputPathsText = uiTextInputPaths.getText().trim();
    if (!StringUtils.isNullOrWhitespace(textInputPathsText)) {
      try {
        String[] split = textInputPathsText.split("\\s*;\\s*");
        Path p = Paths.get(split[0]);
        if (Files.exists(p)) {
          if (Files.isDirectory(p)) {
            fci.setCurrentDirectory(p.toFile());
          } else {
            fci.setCurrentDirectory(p.getParent().toFile());
          }
        }
      } catch (Exception ignored) {
      }
    }
    uiBtnCleanup.addActionListener(e -> {
      List<ValidationResult<?>> errors = new ArrayList<>();
      ValidationResult<List<Path>> inputPaths = pre(getInputPaths(), errors);
      if (inputPaths.isError) {
        log.warn("Could not initiate cleanup");
        return;
      }
      bus.post(new MsgCleanup(uiBtnCleanup, inputPaths.result));
    });

    FormEntry feInputs = new FormEntry("field.input.paths", "Input paths", uiTextInputPaths);
    pInputs.add(feInputs
            .browseButton("Browse", fci, "Select peptide ID files, directories or both",
                pathsSeparator),
        new CC().skip(1).split(2).spanX());
    pInputs.add(uiBtnCleanup, new CC().wrap());
    pInputs.add(feInputs.label(), ccLbl);
    pInputs.add(feInputs.comp, new CC().width("200:300:1000px").spanX().growX().wrap());


    FormEntry feReFiles = new FormEntry("field.input.re-files", "File regex", uiTextReFiles);
    pInputs.add(feReFiles.label(), ccLbl);
    pInputs.add(feReFiles.comp, ccComp);
    FormEntry feReDecoys = new FormEntry("field.input.re-decoys", "Decoys regex", uiTextReDecoys);
    pInputs.add(feReDecoys.label(), ccLbl);
    pInputs.add(feReDecoys.comp, ccCompWrap);

    uiComboDecoysUse.setModel(new DefaultComboBoxModel<>(Arrays.stream(DecoyTreatment.values())
        .map(DecoyTreatment::name).toArray(String[]::new)));
    uiComboDecoysUse.setSelectedItem(DecoyTreatment.FORWARDS_ONLY.toString());
    FormEntry feDecoysUse = new FormEntry("field.input.decoys-use", "Use", uiComboDecoysUse);
    pInputs.add(feDecoysUse.label(), ccLbl);
    pInputs.add(feDecoysUse.comp, new CC().width("100:150px"));
    FormEntry feNoCache = new FormEntry("field.input.no-cache", "No Cache", uiCheckNoCache);
    pInputs.add(feNoCache.label(), ccLbl);
    pInputs.add(feNoCache.comp, ccCompWrap);

    uiComboMassCorrection.setModel(new DefaultComboBoxModel<>(Arrays.stream(MassCorrection.values())
        .map(MassCorrection::name).toArray(String[]::new)));
    uiComboMassCorrection.setSelectedItem(MassCorrection.ZERO_PEAK.toString());
    FormEntry feCalibration = new FormEntry("field.input.calibration", "Calibration",
        uiComboMassCorrection);
    pInputs.add(feCalibration.label(), ccLbl);
    pInputs.add(feCalibration.comp, new CC().width("100:150px").wrap());
    FormEntry feSearchPaths = new FormEntry("field.input.additional-search-paths",
        "Search paths",
        uiTextAdditionalSearchPaths, "<html>These paths will be searched for LCMS files<br/>"
        + "when PEP_ID calibration requested.");
    pInputs.add(feSearchPaths.label(), ccLbl);
    pInputs.add(feSearchPaths.comp, new CC().width("200:300:1000px").span(3).growX());
    JFileChooser fca = new JFileChooser();
    fca.setAcceptAllFileFilterUsed(true);
    fca.setMultiSelectionEnabled(true);
    fca.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fca.setApproveButtonText("Select");
    final String additionalSearchPathsText = uiTextAdditionalSearchPaths.getText().trim();
    if (!StringUtils.isNullOrWhitespace(additionalSearchPathsText)) {
      try {
        String[] split = textInputPathsText.split("\\s*;\\s*");
        Path p = Paths.get(split[0]);
        if (Files.exists(p)) {
          if (Files.isDirectory(p)) {
            fca.setCurrentDirectory(p.toFile());
          } else {
            fca.setCurrentDirectory(p.getParent().toFile());
          }
        }
      } catch (Exception ignored) {
      }
    }
    pInputs.add(feSearchPaths.browseButton("Browse", fca,
        "Additional LCMS files search locations", pathsSeparator), new CC().wrap());

    CC ccGrowX = new CC().growX();
    this.add(pInputs, ccGrowX);

    pKde.setLayout(new MigLayout(lc));
    pKde.setBorder(new TitledBorder("KDE Options"));

    FormEntry feRangeLimitDo = new FormEntry("field.kde.range-limit.do", "Limit dM range",
        uiCheckRangeLimitDo);
    pKde.add(feRangeLimitDo.label(), new CC().split(2).alignX("right"));
    pKde.add(feRangeLimitDo.comp, new CC());
    FormEntry feRangeLimitLo = new FormEntry("field.kde.range-limit.lo", "dM Lo",
        uiSpinnerRangeLimitLo);
    pKde.add(feRangeLimitLo.label(), new CC().alignX("right"));
    final String w1 = "50:70:100px";
    pKde.add(feRangeLimitLo.comp, new CC().width(w1));
    FormEntry feRangeLimitHi = new FormEntry("field.kde.range-limit.hi", "dM Hi", uiSpinnerRangeLimitHi);
    pKde.add(feRangeLimitHi.comp, new CC().width(w1));
    pKde.add(feRangeLimitHi.label(), new CC().spanX().alignX("left").wrap());

    final String uiTextRangeExclusionsOldText = uiTextRangeExclusions.getText();
    uiTextRangeExclusions.setDocument(DocumentFilters.getDigitCommaDotSpaceMinusFitler());
    uiTextRangeExclusions.setText(uiTextRangeExclusionsOldText);
    FormEntry feRangeExclusions = new FormEntry("field.kde.range-excludes", "Exclude dM ranges",
        uiTextRangeExclusions);
    pKde.add(feRangeExclusions.label(), ccLbl);
    pKde.add(feRangeExclusions.comp, new CC().growX().spanX().wrap());

    FormEntry feKdeBandwith = new FormEntry("field.kde.bandwidth", "Bandwidth", uiSpinnerKdeBandwidth,
        "<html>KDE bandwidth. Zero = automatic determination. The smaller the number the less smoothing is applied to data.");
    pKde.add(feKdeBandwith.label(), new CC().split(2));
    pKde.add(feKdeBandwith.comp, new CC());

    FormEntry feKdeStep = new FormEntry("field.kde.dx", "KDE Step", uiSpinnerKdeDx);
//    pKde.add(feKdeStep.label(), new CC().alignX("right").gapBefore("5px").span(2));
//    pKde.add(feKdeStep.comp, ccComp);
    pKde.add(feKdeStep.label(), new CC().alignX("right"));
    pKde.add(feKdeStep.comp, new CC().width(w1));
    FormEntry feKdeMinData = new FormEntry("field.kde.min-data", "Min Data", uiSpinnerKdeMinData);
    pKde.add(feKdeMinData.label(), ccLbl);
    pKde.add(feKdeMinData.comp, new CC().width(w1).wrap());

    FormEntry fePeaksDo = new FormEntry("field.peaks.do", "Detect peaks", uiCheckPeaksDo);
    pKde.add(fePeaksDo.label(), new CC().alignX("right").gapBefore("5px").split(2));
    pKde.add(fePeaksDo.comp, new CC());
    FormEntry fePeaksMinPsms = new FormEntry("field.peaks.min-psms", "Min PSMs", uiSpinnerMinPsm);
    pKde.add(fePeaksMinPsms.label(), new CC().alignX("right"));
    pKde.add(fePeaksMinPsms.comp, new CC().width(w1));
    FormEntry fePeaksMinPct = new FormEntry("field.peaks.min-pct", "Min %",
        uiSpinnerMinPct);
    pKde.add(fePeaksMinPct.label(), ccLbl);
    pKde.add(fePeaksMinPct.comp, new CC().width(w1).wrap());

    FormEntry fePlotStep = new FormEntry("field.plot.step", "Plotting step",
        uiSpinnerPlotStep);
    pKde.add(fePlotStep.label(), new CC().skip(1).alignX("right"));
    pKde.add(fePlotStep.comp, new CC().width(w1).wrap());

    this.add(pKde, ccGrowX);

    pOut.setLayout(new MigLayout(lc));
    pOut.setBorder(new TitledBorder("Output"));
    FormEntry feOutFile = new FormEntry("field.output.path", "Output file", uiTextOutputFile);
    pOut.add(feOutFile.label(), ccLbl);
    pOut.add(feOutFile.comp, new CC().width("200:300:1000px"));
    JFileChooser fco = new JFileChooser();
    fco.setMultiSelectionEnabled(false);
    fco.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    final String outPath = uiTextOutputFile.getText().trim();
    if (!StringUtils.isNullOrWhitespace(outPath)) {
      try {
        fco.setCurrentDirectory(Paths.get(outPath).toFile());
      } catch (Exception ignored) {}
    }
    pOut.add(feOutFile.browseButton("Browse", fco, "Select output directory or file", pathsSeparator));

    this.add(pOut, ccGrowX);

    pButtons.setLayout(new MigLayout(lc));
    pButtons.add(uiBtnRun, new CC());
    pButtons.add(uiBtnStop, new CC());
    pButtons.add(uiBtnConsoleClear, new CC().wrap());
    scrollConsole.setViewportView(console);
    this.add(pButtons, new CC());

    this.add(scrollConsole, new CC().width("100:200:800px").height("100:250:1000px").dockSouth().growY());

    appender.start();
    log.debug("Started LogbackJTextPaneAppender logger");
    appender.setTextPane(console);
    uiBtnRun.addActionListener(e -> run());
    uiBtnConsoleClear.addActionListener(e -> console.setText(""));
    uiBtnStop.addActionListener(e -> {
      log.info("Stop signal sent");
      bus.post(new MsgStop(uiBtnStop));
    });



    bus.register(this);
    // Try to fetch new version info from remote. Runs in a separate thread.
    DeltaMassInfo.checkForNewVersions();

    cache.load(this);
    logSysInfo();
  }

  /** Log system info. */
  private void logSysInfo() {
    final StringBuilder sb = new StringBuilder("System info:\n");
    sb.append(OsUtils.osInfo()).append("\n");
    sb.append(OsUtils.javaInfo());
    log.info(sb.toString());
  }

  @Subscribe
  public void onStop(MsgStop m) {
    synchronized (this) {

      exec.shutdownNow();
    }
  }

  public void run() {
    synchronized (this) {
      MsgStop msgStop = bus.removeStickyEvent(MsgStop.class);
      if (msgStop != null) {
        log.debug("Removed old sticky stop message");
      }
      log.info("Validating inputs");
      balloonRegistry.clear();
      cache.store(this);
      ValidationReport report = validateInputs();
      if (!report.errors.isEmpty()) {
        log.warn("Input validation failed");
        return;
      }
      final LogicPlot logic = new LogicPlot(report.cmd);

      // ask user about overwriting existing output file
      if (report.cmd.optsPeaks.out != null) {
        log.debug("Checking if the output file is ok");
        final Path out = LogicPeaks.getOutputFilePath(report.cmd.optsPeaks.out);
        if (Files.exists(out) && !Files.isDirectory(out)) {
          // if we got here, it's an existing regular file
          // ask user about overwriting
          int choice = JOptionPane.showConfirmDialog(this, new JLabel(
              "<html>Output file exists:<br/>" + out.toString() + "<br/><b>Overwrite?</b>"));
          if (JOptionPane.YES_OPTION != choice) {
            log.info("User chose not to overwrite existing file, stopping.");
            return;
          }
        }
      }

      if (!tasks.stream().allMatch(Future::isDone)) {
        onStop(null);
        if (!exec.isTerminated()) {
          JOptionPane
              .showMessageDialog(this, "Previous task is still running, try again a little later");
          return;
        } else {
          tasks.clear();
        }
      }

      threads.forEach(Thread::interrupt);
      threads.clear();
      Thread thread = new Thread(logic::run);
      threads.add(thread);
      thread.start();

//      exec = Executors.newSingleThreadExecutor();
//      Future<?> future = exec.submit(logic::run);
//      tasks.add(future);
    }
  }

  @Subscribe
  public void onVersionUpdateInfo(MsgVersionUpdateInfo m) {

    final StringBuilder sb = new StringBuilder();
    sb.append(String.format(Locale.ROOT,
        "Your %s version is [%s]<br>\n"
            + "There is a newer version of %s available [%s].<br/>\n"
            + "Please <a href=\"%s\">click here</a> to download a newer one.<br/>"
            + "Full link:<br/>"
            + "<a href=\"%s\">%s</a>",
        m.programName, m.currentVersion, m.programName, m.newVersion, m.downloadUrl, m.downloadUrl,
        m.downloadUrl));
    JEditorPane clickableHtml = SwingUtils.createClickableHtml(sb.toString());
    clickableHtml.setBackground(new JLabel().getBackground());
    JOptionPane.showMessageDialog(this, clickableHtml,
        "New version available", JOptionPane.INFORMATION_MESSAGE);
  }

  private ValidationReport validateInputs() {
    List<ValidationResult<?>> errors = new ArrayList<>();

    CommandPlot cmd = new CommandPlot();
    UserOptsInputFiles optsInputs = new UserOptsInputFiles();
    cmd.optsInputFiles = optsInputs;

    optsInputs.noCache = uiCheckNoCache.isSelected();

    ValidationResult<List<Double>> exclusionRanges = pre(getExclusionRanges(), errors);
    if (!exclusionRanges.isError) {
      optsInputs.excludeMzRanges = exclusionRanges.result;
    }

    ValidationResult<Pattern> regexFiles = pre(getRegexFiles(), errors);
    if (!regexFiles.isError && regexFiles.result != null) {
      optsInputs.fileRegex = regexFiles.result;
    }

    ValidationResult<Pattern> regexDecoys = pre(getRegexDecoys(), errors);
    if (!regexDecoys.isError && regexDecoys.result != null) {
      optsInputs.decoyRegex = regexDecoys.result;
    }

    ValidationResult<DecoyTreatment> decoyTreatment = pre(getDecoyTreatment(), errors);
    if (!decoyTreatment.isError) {
      optsInputs.decoyTreatment = decoyTreatment.result;
    }

    ValidationResult<List<Path>> inputPaths = pre(getInputPaths(), errors);
    if (!inputPaths.isError) {
      optsInputs.inputFiles = inputPaths.result;
    }

    ValidationResult<Double> massCutoffLo = pre(getMassCutoffLo(), errors);
    if (!massCutoffLo.isError) {
      optsInputs.mLo = massCutoffLo.result;
    }

    ValidationResult<Double> massCutoffHi = pre(getMassCutoffHi(), errors);
    if (!massCutoffHi.isError) {
      optsInputs.mHi = massCutoffHi.result;
    }

    ValidationResult<List<Path>> searchPaths = pre(getSearchPaths(), errors);
    if (!searchPaths.isError) {
      optsInputs.additionalSearchPaths = searchPaths.result;
    }

    ValidationResult<MassCorrection> massCorrection = pre(getMassCorrection(), errors);
    if (!massCorrection.isError) {
      optsInputs.massCorrection = massCorrection.result;
    }

    UserOptsKde optsKde = new UserOptsKde();
    cmd.optsKde = optsKde;

    optsKde.bandwidth = (Double) uiSpinnerKdeBandwidth.getValue();
    optsKde.step = (Double) uiSpinnerKdeDx.getValue();
    optsKde.minData = (Integer) uiSpinnerKdeMinData.getValue();

    UserOptsPeaks optsPeaks = new UserOptsPeaks();
    cmd.optsPeaks = optsPeaks;

    optsPeaks.minPsmsPerGmm = (Integer) uiSpinnerMinPsm.getValue();
    optsPeaks.minPeakPct = (Double) uiSpinnerMinPct.getValue();
    ValidationResult<Path> outputPath = pre(getOutputPath(), errors);
    if (!outputPath.isError) {
      optsPeaks.out = outputPath.result;
    }

    UserOptsPlot optsPlot = new UserOptsPlot();
    cmd.optsPlot = optsPlot;
    optsPlot.noPeaks = !uiCheckPeaksDo.isSelected();
    optsPlot.step = (Double) uiSpinnerPlotStep.getValue();
    optsPlot.exitOnClose = false;

    return new ValidationReport(errors, cmd);
  }

  private class ValidationReport {
    final List<ValidationResult<?>> errors;
    final CommandPlot cmd;

    private ValidationReport(
        List<ValidationResult<?>> errors, CommandPlot cmd) {
      this.errors = errors;
      this.cmd = cmd;
    }
  }

  private <T> ValidationResult<T> pre(ValidationResult<T> result, List<ValidationResult<?>> errors) {
    if (result.isError) {
      errors.add(result);
      balloonRegistry.createTip(result.component, result.errorMessage);
    }
    return result;
  }

  private ValidationResult<Double> getMassCutoffLo() {
    boolean isLimit = uiCheckRangeLimitDo.isSelected();
    if (!isLimit) {
      return new ValidationResult<>(null);
    }
    double lo = (Double)uiSpinnerRangeLimitLo.getValue();
    double hi = (Double)uiSpinnerRangeLimitHi.getValue();
    if (hi < lo) {
      return new ValidationResult<>("Low must be < high", uiSpinnerRangeLimitLo);
    }
    return new ValidationResult<>(lo);
  }

  private ValidationResult<Double> getMassCutoffHi() {
    boolean isLimit = uiCheckRangeLimitDo.isSelected();
    if (!isLimit) {
      return new ValidationResult<>(null);
    }
    double lo = (Double)uiSpinnerRangeLimitLo.getValue();
    double hi = (Double)uiSpinnerRangeLimitHi.getValue();
    if (hi < lo) {
      return new ValidationResult<>("High must be > low", uiSpinnerRangeLimitHi);
    }
    return new ValidationResult<>(hi);
  }

  private ValidationResult<List<Path>> getInputPaths() {
    final UiText c = uiTextInputPaths;
    String text = c.getNonGhostText();
    if (StringUtils.isNullOrWhitespace(text)) {
      return new ValidationResult<>("Can't be left empty", c);
    }

    String[] paths = text.split(pathsSeparator);
    List<Path> res = new ArrayList<>();
    for (String path : paths) {
      try {
        Path p = Paths.get(path);
        if (!p.toFile().exists()) {
          return new ValidationResult<>("Not all given paths exist", c);
        }
        res.add(p);
      } catch (Exception e) {
        return new ValidationResult<>("Not all given paths are valid", c);
      }
    }
    return new ValidationResult<>(res);
  }

  private ValidationResult<List<Path>> getSearchPaths() {
    final UiText c = uiTextAdditionalSearchPaths;
    String text = uiTextAdditionalSearchPaths.getText();
    if (StringUtils.isNullOrWhitespace(text) || text.equals(c.getGhostText())) {
      return new ValidationResult<>(Collections.emptyList());
    }

    String[] paths = text.split(pathsSeparator);
    List<Path> res = new ArrayList<>();
    for (String path : paths) {
      try {
        Path p = Paths.get(path);
        if (!p.toFile().exists()) {
          return new ValidationResult<>("Not all given paths exist", c);
        }
        res.add(p);
      } catch (Exception e) {
        return new ValidationResult<>("Not all given paths are valid", c);
      }
    }
    return new ValidationResult<>(res);
  }

  private ValidationResult<DecoyTreatment> getDecoyTreatment() {
    try {
      return new ValidationResult<>(DecoyTreatment.valueOf(uiComboDecoysUse.asString()));
    } catch (Exception e) {
      return new ValidationResult<>(e.getMessage(), uiComboDecoysUse);
    }
  }

  private ValidationResult<MassCorrection> getMassCorrection() {
    try {
      return new ValidationResult<>(MassCorrection.valueOf(uiComboMassCorrection.asString()));
    } catch (Exception e) {
      return new ValidationResult<>(e.getMessage(), uiComboMassCorrection);
    }
  }

  private ValidationResult<Path> getOutputPath() {
    final UiText c = uiTextOutputFile;
    final String t = c.getNonGhostText();
    if (!StringUtils.isNullOrWhitespace(t)) {
      try {
        return new ValidationResult<>(Paths.get(t));
      } catch (Exception e) {
        return new ValidationResult<>("Not a valid path", c);
      }
    }
    return new ValidationResult<>(null);
  }

  /**
   * @return Null when input text field is empty or whitespace.
   */
  private ValidationResult<Pattern> getRegexFiles() {
    final String text = uiTextReFiles.getText();
    try {
      if (StringUtils.isNullOrWhitespace(text)) {
        return new ValidationResult<>(null);
      } else {
        return new ValidationResult<>(Pattern.compile(text));
      }
    } catch (Exception e) {
      return new ValidationResult<>(e.getMessage(), uiTextReFiles);
    }
  }

  /**
   * @return Null when input text field is empty or whitespace.
   */
  private ValidationResult<Pattern> getRegexDecoys() {
    final String text = uiTextReDecoys.getText();
    try {
      return StringUtils.isNullOrWhitespace(text) ? new ValidationResult<>(null) : new ValidationResult<>(Pattern.compile(text));
    } catch (Exception e) {
      return new ValidationResult<>(e.getMessage(), uiTextReDecoys);
    }
  }

  private ValidationResult<List<Double>> getExclusionRanges() {
    final String text = uiTextRangeExclusions.getText().trim();
    String[] pairs = text.split("\\s*,\\s*");
    List<Double> res = new ArrayList<>();
    for (String pair : pairs) {
      if (StringUtils.isNullOrWhitespace(pair)) {
        continue;
      }
      String[] numbers = pair.trim().split("\\s+");
      if (numbers.length != 2) {
        return new ValidationResult<>("Not 2 numbers between commas", uiTextRangeExclusions);
      }
      try {
        double v1 = Double.parseDouble(numbers[0]);
        double v2 = Double.parseDouble(numbers[1]);
        if (v2 <= v1) {
          return new ValidationResult<>("2nd number in range must be greater than 1st", uiTextRangeExclusions);
        }
        res.add(v1);
        res.add(v2);
      } catch (Exception e) {
        return new ValidationResult<>("Could not parse number", uiTextRangeExclusions);
      }
    }
    return new ValidationResult<>(res);
  }

  @Subscribe
  public void onFlushGuiCache(MsgFlushGuiCache m) {
    log.debug("Flushing cache");
    cache.flush();
  }

  @Subscribe
  public void onCleanup(MsgCleanup m) {
    CommandClean cmd = new CommandClean();
    cmd.inputFiles = m.paths;
    LogicClean logicClean = new LogicClean(cmd);
    List<Path> toDelete = logicClean.getScheduledForDeletion();

    String[] columns = {"File to delete"};
    String[][] data = new String[toDelete.size()][1];
    for (int i = 0; i < toDelete.size(); i++) {
      data[i][0] = toDelete.get(i).toString();
    }
    DefaultTableModel model = new DefaultTableModel(data, columns);
    JTable table = new JTable(model);
    int confirmation = JOptionPane.showConfirmDialog(this, SwingUtils.wrapInScrollForDialog(table),
        "Delete these files?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    if (JOptionPane.YES_OPTION == confirmation) {
      for (Path path : toDelete) {
        try {
          Files.deleteIfExists(path);
        } catch (Exception e) {
          log.warn("Could not delete file: " + path.toString(), e);
        }
      }
    }
  }

  @Subscribe
  public void onRunPlot(MsgRunPlot m) {
    run();
  }

  private static class ValidationResult<T> {
    final T result;
    final boolean isError;
    final String errorMessage;
    final JComponent component;

    public ValidationResult(T result) {
      this.result = result;
      isError = false;
      errorMessage = null;
      component = null;
    }

    public ValidationResult(String errorMessage, JComponent component) {
      this.result = null;
      this.isError = true;
      this.errorMessage = errorMessage;
      this.component = component;
    }

    public ValidationResult(T result, boolean isError, String errorMessage, JComponent component) {
      this.result = result;
      this.isError = isError;
      this.errorMessage = errorMessage;
      this.component = component;
    }
  }

  private class BalloonRegistry {
    private Map<JComponent, BalloonTip> tips = new HashMap<>();
    private final Object lock = new Object();

    public void clear() {
      synchronized (lock) {
        for (BalloonTip tip : tips.values()) {
          tip.closeBalloon();
        }
        tips = new HashMap<>();
      }
    }

    public void createTip(JComponent attachTo, String text) {
      synchronized (lock) {
        BalloonTip oldTip = tips.get(attachTo);
        if (oldTip != null) {
          oldTip.closeBalloon();
        }
        BalloonTip tip = new BalloonTip(attachTo, text, DeltaMassOptionsForm.this.style, true);
        tips.put(attachTo, tip);
        tip.setVisible(true);
      }
    }
  }
}
