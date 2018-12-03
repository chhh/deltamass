/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umich.opensearch.kde.gui;

import com.beust.jcommander.ParameterException;
import com.github.chhh.utils.LogUtils;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.swing.TextConsole;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.opensearch.kde.KDEMain;
import umich.opensearch.kde.OpenSearchParams;
import umich.opensearch.kde.Version;
import umich.opensearch.kde.jfree.JFreeChartPlot;
import umich.opensearch.kde.logging.LogbackJTextPaneAppender;
import umich.opensearch.kde.params.DecoyTreatment;
import umich.opensearch.kde.params.KDEKernelType;
import umich.opensearch.kde.params.MassCorrection;
import umich.opensearch.kde.params.PepXmlScore;
import umich.opensearch.kde.params.ScorePredicateFactoryParameter;
import umich.opensearch.kde.params.denoise.Denoising;
import umich.opensearch.kde.params.predicates.ComparisonType;
import umich.opensearch.kde.params.predicates.ScorePredicate;

/**
 * @author dmitriya
 */
public class KDEOptionsGui extends javax.swing.JFrame {

  public static final String FRAME_NAME = "OpenSearch KDE";
  public static final List<String> ICON_NAMES = Arrays.asList(
      "delta_logo_16.png", "delta_logo_32.png", "delta_logo_48.png",
      "delta_logo_64.png", "delta_logo_96.png", "delta_logo_128.png", "delta_logo_256.png");
  public static final String PROP_PATHIN = "pathIn";
  public static final String PROP_PATHOUT = "pathOut";
  private static final Logger log = LoggerFactory.getLogger(KDEOptionsGui.class);
  private static final String SPLIT = ", ";
  private static final String STR_PARAM_ERROR = "Parameter error";
  private static final String TEMP_FILE_NAME = "opensearchkde.cache";
  private static double DEFAULT_BANDWIDTH_FOR_GUI = 0.001;
  private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
      this);
  protected ExecutorService exec;
  private LogbackJTextPaneAppender appender;
  private TextConsole console;
  private OpenSearchParams paramsDefault = new OpenSearchParams();
  private String pathIn;
  private String pathOut;
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox boxClearDmRange;
  private javax.swing.JCheckBox boxDetectPeaks;
  private javax.swing.JCheckBox boxDoPlot;
  private javax.swing.JCheckBox boxForce;
  private javax.swing.JCheckBox boxMassDependent;
  private javax.swing.JCheckBox boxUseCache;
  private javax.swing.JButton btnBrowseFilterFile;
  private javax.swing.JButton btnConsoleClear;
  private javax.swing.JButton btnDeleteCache;
  private javax.swing.JButton btnFileIn;
  private javax.swing.JButton btnFileOut;
  private javax.swing.JButton btnFilterAdd;
  private javax.swing.JButton btnFilterRemove;
  private javax.swing.JButton btnGmmReset;
  private javax.swing.JButton btnGo;
  private javax.swing.JComboBox<DecoyTreatment> comboDecoyTreatment;
  private javax.swing.JComboBox<ComparisonType> comboFilterOperation;
  private javax.swing.JComboBox<PepXmlScore> comboFilterScoreName;
  private javax.swing.JComboBox<KDEKernelType> comboKernelType;
  private javax.swing.JComboBox<MassCorrection> comboMassCorrection;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel10;
  private javax.swing.JLabel jLabel11;
  private javax.swing.JLabel jLabel12;
  private javax.swing.JLabel jLabel13;
  private javax.swing.JLabel jLabel14;
  private javax.swing.JLabel jLabel15;
  private javax.swing.JLabel jLabel16;
  private javax.swing.JLabel jLabel17;
  private javax.swing.JLabel jLabel18;
  private javax.swing.JLabel jLabel19;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel20;
  private javax.swing.JLabel jLabel21;
  private javax.swing.JLabel jLabel22;
  private javax.swing.JLabel jLabel23;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JLabel jLabel9;
  private javax.swing.JPanel panelCache;
  private javax.swing.JPanel panelFilters;
  private javax.swing.JPanel panelKDE;
  private javax.swing.JPanel panelOutput;
  private javax.swing.JPanel panelPeakDetection;
  private javax.swing.JScrollPane scrollPaneConsole;
  private javax.swing.JSpinner spinnerClearDmHi;
  private javax.swing.JSpinner spinnerClearDmLo;
  private javax.swing.JSpinner spinnerMassWindowForBandwidthEstimate;
  private javax.swing.JTextField textFilterFile;
  private javax.swing.JTextField txtBandwidths;
  private javax.swing.JTextField txtCacheExt;
  private javax.swing.JTextField txtDecoyPrefix;
  private javax.swing.JTextField txtFileIn;
  private javax.swing.JTextField txtFileOut;
  private javax.swing.JTextField txtFileRegex;
  private javax.swing.JTextField txtFilter;
  private javax.swing.JTextField txtFilterValue;
  private javax.swing.JTextField txtGmmMinPts;
  private javax.swing.JTextField txtGmmNumPts;
  private javax.swing.JTextField txtMassForBandwidthEstimate;
  private javax.swing.JTextField txtMassHi;
  private javax.swing.JTextField txtMassLo;
  private javax.swing.JTextField txtMassStep;
  private javax.swing.JTextField txtWeightScoreName;
  /**
   * Creates new form KDEOptionsGui
   */
  public KDEOptionsGui() {
    initComponents();
    initMore();
  }

  /**
   * @param params can be null, otherwise these params will be used as defaults in GUI.
   */
  public static void initAndRun(final OpenSearchParams params) {
    /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
     */
    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(KDEOptionsGui.class.getName())
          .log(java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        KDEOptionsGui kdeOptionsGui = new KDEOptionsGui();
        kdeOptionsGui.pack();
        SwingUtils.centerFrame(kdeOptionsGui);
        kdeOptionsGui.setVisible(true);
        if (params != null) {
          kdeOptionsGui.setParamsDefault(params);
        }
        // set up logging to print to the JTextPane console

      }
    });
  }

  private static List<String> splitTrim(String input, String sep) {
    String[] split = input.split(sep);
    List<String> strings = new ArrayList<>(split.length);
    for (int i = 0; i < split.length; i++) {
      split[i] = split[i].trim();
      if (!split[i].isEmpty()) {
        strings.add(split[i]);
      }
    }
    return strings;
  }

  private void initMore() {
    umich.opensearch.kde.logging.LogHelper.configureJavaUtilLogging();

    setTitle(FRAME_NAME + " v" + Version.version);

    exec = Executors.newCachedThreadPool();
    SwingUtils.setFrameIcons(this, ICON_NAMES, KDEOptionsGui.class);
    console = new TextConsole();
    scrollPaneConsole.setViewportView(console);

    appender = new LogbackJTextPaneAppender();
    appender.start();
    log.debug("Started LogbackJTextPaneAppender logger");
    appender.setTextPane(console);
  }

  public OpenSearchParams getParamsDefault() {
    return paramsDefault;
  }

  public void setParamsDefault(OpenSearchParams params) {
    paramsDefault = params;
  }

  private SpinnerModel createSpinnerModel() {
    Double window = getParamsDefault().getAutoBandwidthWindow();
    double w = window != null ? window : 0.5;
    SpinnerNumberModel model = new SpinnerNumberModel(w, 0.0, 1.0, 0.05);
    return model;
  }

  private SpinnerModel createSpinnerModelMassStep() {
    double step = getParamsDefault().getMzStep();
    SpinnerNumberModel model = new SpinnerNumberModel(step, 0.000001, 1.0, 0.0001);
    return model;
  }

  private String createTextMassStep() {
    return String.format("%s", getParamsDefault().getMzStep());
  }

  private String createTextGmmNumPts() {
    return String.format("%d", getParamsDefault().getGmmNumEntries());
  }

  private String createTextGmmMinPts() {
    return String.format("%d", getParamsDefault().getGmmMinEntries());
  }

  private String createTextBandwidths() {
    List<Double> bandwidths = getParamsDefault().getBandwidths();
    if (bandwidths.isEmpty() && !getParamsDefault().isDynamicBandwidthEstimation()) {
      bandwidths.add(DEFAULT_BANDWIDTH_FOR_GUI);
    }

    StringBuilder sb = new StringBuilder();
    if (!bandwidths.isEmpty()) {
      DecimalFormat format = new DecimalFormat("0.##################");
      for (int i = 0; i < bandwidths.size(); i++) {
        sb.append(format.format(bandwidths.get(i)));
        if (i < bandwidths.size() - 1) {
          sb.append(',');
        }
      }
    }
    return sb.toString();
  }

  private SpinnerModel createFilterSpinnerModel() {
    SpinnerNumberModel model = new SpinnerNumberModel(0.95, null, null, 0.01);
    return model;
  }

  private ComboBoxModel<KDEKernelType> createComboKernelType() {
    DefaultComboBoxModel<KDEKernelType> model = new DefaultComboBoxModel<>(KDEKernelType.values());
    model.setSelectedItem(getParamsDefault().getKernelType());
    return model;
  }

  private ComboBoxModel<DecoyTreatment> createComboDecoyTreatment() {
    DefaultComboBoxModel<DecoyTreatment> model = new DefaultComboBoxModel<>(
        DecoyTreatment.values());
    model.setSelectedItem(getParamsDefault().getDecoysTreatment());
    return model;
  }

  private ComboBoxModel<Denoising> createComboDenoisingType() {
    DefaultComboBoxModel<Denoising> model = new DefaultComboBoxModel<>(Denoising.values());
    model.setSelectedItem(getParamsDefault().getDenoising());
    return model;
  }

  private ComboBoxModel<PepXmlScore> createComboFilterScoreName() {
    DefaultComboBoxModel<PepXmlScore> model = new DefaultComboBoxModel<>(PepXmlScore.values());
    model.setSelectedItem(PepXmlScore.peptideprophet);
    return model;
  }

  private ComboBoxModel<MassCorrection> createComboMassCorrection() {
    DefaultComboBoxModel<MassCorrection> model = new DefaultComboBoxModel<>(
        MassCorrection.values());
    model.setSelectedItem(getParamsDefault().getMassCorrection());
    return model;
  }

  private ComboBoxModel<ComparisonType> createComboComparisonType() {
    DefaultComboBoxModel<ComparisonType> model = new DefaultComboBoxModel<>(
        ComparisonType.values());
    model.setSelectedItem(ComparisonType.GREATER_OR_EQUAL);
    return model;
  }

  /**
   * Get the value of pathIn
   *
   * @return the value of pathIn
   */
  public String getPathIn() {
    return pathIn;
  }

  /**
   * Set the value of pathIn
   *
   * @param pathIn new value of pathIn
   */
  public void setPathIn(String pathIn) {
    String oldPathIn = this.pathIn;
    this.pathIn = pathIn;
    propertyChangeSupport.firePropertyChange(PROP_PATHIN, oldPathIn, pathIn);
  }

  private String getPreviousPathsIn() {
    KDEGuiProps props = KDEGuiProps.loadFromTemp();
    return props == null ? "" : props.getProperty(KDEGuiProps.PROP_FILE_IN);
  }

  private String getPreviousPathsOut() {
    KDEGuiProps props = KDEGuiProps.loadFromTemp();
    return props == null ? "" : props.getProperty(KDEGuiProps.PROP_FILE_OUT);
  }

  /**
   * Add PropertyChangeListener.
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Creates parameter object for OKDE based on the state of the GUI.
   */
  private OpenSearchParams createOpenSearchParams() {

    OpenSearchParams params = new OpenSearchParams();

    String filesInStr = txtFileIn.getText().trim();
    if (StringUtils.isBlank(filesInStr)) {
      JOptionPane.showMessageDialog(this, "Select some files or a directory", STR_PARAM_ERROR,
          JOptionPane.INFORMATION_MESSAGE);
      return null;
    }

    List<Path> inFilePath = new ArrayList<>();
    List<String> filePaths = splitTrim(filesInStr, ",");
    for (int i = 0; i < filePaths.size(); i++) {
      String filePath = filePaths.get(i);
      Path p = Paths.get(filePath).toAbsolutePath();
      if (!Files.exists(p)) {
        JOptionPane.showMessageDialog(this, String.format(
            "Not an existing path: %s", p.toString()), STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
      inFilePath.add(p);
    }
    params.setInFilePath(inFilePath);

    params.setDecoysTreatment((DecoyTreatment) comboDecoyTreatment.getModel().getSelectedItem());

    params.setDeleteCache(false);
    params.setUseCache(boxUseCache.isSelected());
    params.setForce(boxForce.isSelected());
    if (params.isDeleteCache()) {
      return params;
    }
    if (params.isUseCache()) {
      String cacheExtText = txtCacheExt.getText().trim();
      if (cacheExtText.isEmpty()) {
        JOptionPane.showMessageDialog(this, String.format(
            "Can't leave 'Cache file ext' field empty when 'Use cache' specified"), STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
      params.setCacheExt(cacheExtText);
    }

    String regex = txtFileRegex.getText();
    if (regex.isEmpty()) {
      JOptionPane.showMessageDialog(this, String.format(
          "Can't leave File regex empty"), STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
      return null;
    } else {
      params.setInFileRegex(regex);
    }

    //params.setFollowSymlinks(boxFollowSymlinks.isSelected());
    params.setPlotKde(boxDoPlot.isSelected());
    List<String> decoyPrefixes = splitTrim(txtDecoyPrefix.getText(), ",");
    params.setDecoyProtPrefix(decoyPrefixes);

    if (boxClearDmRange.isSelected()) {
      double dmLo = (Double) spinnerClearDmLo.getValue();
      double dmHi = (Double) spinnerClearDmHi.getValue();
      if (dmHi <= dmLo) {
        JOptionPane.showMessageDialog(this, String.format(
            "Can't leave File regex empty"), STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
      }
      params.setClearRange(Arrays.asList(dmLo, dmHi));
    }

    params.setDynamicBandwidthEstimation(boxMassDependent.isSelected());
    String massText = txtMassForBandwidthEstimate.getText();
    if (!massText.isEmpty()) {
      try {
        params.setAutoBandwithTarget(Double.parseDouble(massText));
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, String.format(
            "Could not parse numeric value from 'Mass(h)' field.\n%s", e.getMessage()),
            STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    params.setAutoBandwidthWindow((Double) spinnerMassWindowForBandwidthEstimate.getValue());
    params.setKernelType((KDEKernelType) comboKernelType.getModel().getSelectedItem());

    String bandwidthsText = txtBandwidths.getText().trim();
    if (!bandwidthsText.isEmpty()) {
      List<String> bandwidths = splitTrim(bandwidthsText, ",");
      List<Double> bs = new ArrayList<>(bandwidths.size());
      for (String bandwidth : bandwidths) {
        try {
          bs.add(Double.parseDouble(bandwidth));
        } catch (NumberFormatException e) {
          JOptionPane.showMessageDialog(this, String.format(
              "Could not parse numeric value of provided bandwidth.\n%s", e.getMessage()),
              STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
          return null;
        }
      }
      params.setBandwidths(bs);
    } else {
      params.setBandwidths(Collections.emptyList());
    }

    String massLoStr = txtMassLo.getText().trim();
    if (!massLoStr.isEmpty()) {
      try {
        params.setMzLo(Double.parseDouble(massLoStr));
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, String.format(
            "Incorrect number format for low mass range.\n%s", e.getMessage()), STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    String massHiStr = txtMassHi.getText().trim();
    if (!massHiStr.isEmpty()) {
      try {
        params.setMzHi(Double.parseDouble(massHiStr));
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, String.format(
            "Incorrect number format for high mass range.\n%s", e.getMessage()), STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    String massStepStr = txtMassStep.getText().trim();
    if (!massStepStr.isEmpty()) {
      try {
        params.setMzStep(Double.parseDouble(massStepStr));
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, String.format(
            "Incorrect number format for mass step.\n%s", e.getMessage()), STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    params.setMassCorrection((MassCorrection) comboMassCorrection.getSelectedItem());

    String weight = txtWeightScoreName.getText().trim();
    if (!weight.isEmpty()) {
      params.setWeightsScoreName(weight);
    }

    String filterText = txtFilter.getText().trim();
    if (!filterText.isEmpty()) {
      List<ScorePredicate.Factory> scorePredicates = new ArrayList<>();
      ScorePredicateFactoryParameter validator = new ScorePredicateFactoryParameter();
      List<String> filters = splitTrim(filterText, ",");
      for (String filter : filters) {
        try {
          validator.validate("Score filter", filter);
          ScorePredicate.Factory factory = validator.convert(filter);
          scorePredicates.add(factory);
        } catch (ParameterException e) {
          JOptionPane.showMessageDialog(this, String.format(
              "Filter string incorrect.\n%s", e.getMessage()), STR_PARAM_ERROR,
              JOptionPane.WARNING_MESSAGE);
          return null;
        }
      }
      params.setScorePredicateFactories(scorePredicates);
    }

    params.setDetectPeaks(boxDetectPeaks.isSelected());

    if (params.isDetectPeaks()) {
      if (!txtGmmNumPts.getText().isEmpty()) {
        try {
          int gmmNumPts = Integer.parseInt(txtGmmNumPts.getText());
          if (gmmNumPts < 100) {
            JOptionPane.showMessageDialog(this,
                String.format("Peak detection: The allowed minimum for Num pts is 100.\n"),
                STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
            return null;
          }
          params.setGmmNumEntries(gmmNumPts);
        } catch (NumberFormatException e) {
          JOptionPane.showMessageDialog(this,
              String.format("Peak detection: Could not parse Num pts as an integer.\n"),
              STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
          return null;
        }
      } else {
        JOptionPane.showMessageDialog(this, String.format("Peak detection: Num pts is empty.\n"),
            STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
        return null;
      }

      if (!txtGmmMinPts.getText().isEmpty()) {
        try {
          int gmmMinPts = Integer.parseInt(txtGmmMinPts.getText());
          params.setGmmMinEntries(gmmMinPts);
        } catch (NumberFormatException e) {
          JOptionPane.showMessageDialog(this,
              String.format("Peak detection: Could not parse Min pts as an integer.\n"),
              STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
          return null;
        }
      } else {
        JOptionPane.showMessageDialog(this, String.format("Peak detection: Min pts is empty.\n"),
            STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    String outFileText = txtFileOut.getText().trim();
    if (!outFileText.isEmpty()) {

      try {
        Path outFilePath = Paths.get(outFileText).toAbsolutePath();

        if (!params.isForce() && Files.exists(outFilePath) && Files.isRegularFile(outFilePath)) {
          JOptionPane.showMessageDialog(this, String.format(
              "Cowardly refusing to overwrite existing output file."), STR_PARAM_ERROR,
              JOptionPane.WARNING_MESSAGE);
          return null;
        }
        params.setOutFilePath(outFilePath);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Bad output file path", STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    int countBandwidths = 0;
    if (params.isDynamicBandwidthEstimation()) {
      countBandwidths++;
    }
    if (params.getAutoBandwithTarget() != null) {
      countBandwidths++;
    }
    if (params.getBandwidths() != null && params.getBandwidths().size() > 0) {
      countBandwidths += params.getBandwidths().size();
    }
    if (countBandwidths == 0) {
      String msg = String.format(
          "Must provide at least one bandwidth.\nSet it to 'dynamic' or set a mass to " +
              "be used for automatic estimation or provide your own values.\n" +
              "Check out 'Mass dependent', 'Mass(h)' and 'List of bandwidths' parameters in 'KDE' section");
      JOptionPane.showMessageDialog(this, msg, STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
      return null;
    }

    try {
      params.validate();
    } catch (ParameterException e) {
      String msg = String
          .format("Hold on, something is not right with the parameters.\n%s", e.getMessage());
      JOptionPane.showMessageDialog(this, msg, STR_PARAM_ERROR, JOptionPane.WARNING_MESSAGE);
      return null;
    }

    return params;
  }

  protected void run() {
    if (!SwingUtils.isGraphicalEnvironmentAvailable()) {
      System.err.println("Graphcal environment is needed to run the GUI.");
    }

    System.gc(); // just in case

    final OpenSearchParams params = createOpenSearchParams();
    if (params == null) {
      return;
    } else {
      try {
        params.validate();
      } catch (ParameterException e) {
        JOptionPane.showMessageDialog(this, String.format(
            "Not all is well with parameters.\n%s", e.getMessage()), STR_PARAM_ERROR,
            JOptionPane.WARNING_MESSAGE);
        return;
      }
    }
    final KDEMain kdeMain = new KDEMain();

    kdeMain.setChartCloseOption(JFreeChartPlot.ChartCloseOption.DISPOSE);

    Runnable runnable = () -> kdeMain.run(params);
    REHandler reHandler = new REHandler(runnable, System.err, console);
    exec.execute(reHandler);

  }

  /**
   * Remove PropertyChangeListener.
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  /**
   * Get the value of pathOut
   *
   * @return the value of pathOut
   */
  public String getPathOut() {
    return pathOut;
  }

  /**
   * Set the value of pathOut
   *
   * @param pathOut new value of pathOut
   */
  public void setPathOut(String pathOut) {
    String oldPathOut = this.pathOut;
    this.pathOut = pathOut;
    propertyChangeSupport.firePropertyChange(PROP_PATHOUT, oldPathOut, pathOut);
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    txtFileIn = new javax.swing.JTextField();
    btnFileIn = new javax.swing.JButton();
    jLabel1 = new javax.swing.JLabel();
    txtFileRegex = new javax.swing.JTextField();
    jLabel2 = new javax.swing.JLabel();
    txtDecoyPrefix = new javax.swing.JTextField();
    panelCache = new javax.swing.JPanel();
    boxUseCache = new javax.swing.JCheckBox();
    jLabel3 = new javax.swing.JLabel();
    txtCacheExt = new javax.swing.JTextField();
    boxForce = new javax.swing.JCheckBox();
    panelKDE = new javax.swing.JPanel();
    boxMassDependent = new javax.swing.JCheckBox();
    jLabel4 = new javax.swing.JLabel();
    txtMassForBandwidthEstimate = new javax.swing.JTextField();
    jLabel5 = new javax.swing.JLabel();
    spinnerMassWindowForBandwidthEstimate = new javax.swing.JSpinner();
    jLabel6 = new javax.swing.JLabel();
    comboKernelType = new javax.swing.JComboBox<>();
    jLabel7 = new javax.swing.JLabel();
    txtBandwidths = new javax.swing.JTextField();
    jLabel8 = new javax.swing.JLabel();
    txtWeightScoreName = new javax.swing.JTextField();
    jLabel12 = new javax.swing.JLabel();
    txtMassLo = new javax.swing.JTextField();
    jLabel13 = new javax.swing.JLabel();
    txtMassHi = new javax.swing.JTextField();
    jLabel14 = new javax.swing.JLabel();
    jLabel15 = new javax.swing.JLabel();
    txtMassStep = new javax.swing.JTextField();
    jLabel16 = new javax.swing.JLabel();
    comboMassCorrection = new javax.swing.JComboBox<>();
    boxClearDmRange = new javax.swing.JCheckBox();
    jLabel19 = new javax.swing.JLabel();
    spinnerClearDmLo = new javax.swing.JSpinner();
    jLabel20 = new javax.swing.JLabel();
    spinnerClearDmHi = new javax.swing.JSpinner();
    jLabel21 = new javax.swing.JLabel();
    boxDoPlot = new javax.swing.JCheckBox();
    panelOutput = new javax.swing.JPanel();
    jLabel9 = new javax.swing.JLabel();
    txtFileOut = new javax.swing.JTextField();
    btnFileOut = new javax.swing.JButton();
    jLabel10 = new javax.swing.JLabel();
    btnGo = new javax.swing.JButton();
    panelFilters = new javax.swing.JPanel();
    jLabel11 = new javax.swing.JLabel();
    txtFilter = new javax.swing.JTextField();
    comboFilterScoreName = new javax.swing.JComboBox<>();
    comboFilterOperation = new javax.swing.JComboBox<>();
    btnFilterAdd = new javax.swing.JButton();
    btnFilterRemove = new javax.swing.JButton();
    txtFilterValue = new javax.swing.JTextField();
    btnConsoleClear = new javax.swing.JButton();
    panelPeakDetection = new javax.swing.JPanel();
    boxDetectPeaks = new javax.swing.JCheckBox();
    btnGmmReset = new javax.swing.JButton();
    jLabel17 = new javax.swing.JLabel();
    txtGmmNumPts = new javax.swing.JTextField();
    jLabel18 = new javax.swing.JLabel();
    txtGmmMinPts = new javax.swing.JTextField();
    scrollPaneConsole = new javax.swing.JScrollPane();
    jLabel22 = new javax.swing.JLabel();
    comboDecoyTreatment = new javax.swing.JComboBox<>();
    btnDeleteCache = new javax.swing.JButton();
    textFilterFile = new javax.swing.JTextField();
    jLabel23 = new javax.swing.JLabel();
    btnBrowseFilterFile = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("Open Search Mass Diff KDE");
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowOpened(java.awt.event.WindowEvent evt) {
        formWindowOpened(evt);
      }
    });

    txtFileIn.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        txtFileInActionPerformed(evt);
      }
    });

    btnFileIn.setText("Browse");
    btnFileIn.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFileInActionPerformed(evt);
      }
    });

    jLabel1.setText("File regex");
    jLabel1.setToolTipText(txtFileRegex.getToolTipText());

    txtFileRegex.setText(getParamsDefault().getInFileRegex());
    txtFileRegex.setToolTipText("A regular expression, not just a glob");
    txtFileRegex.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        txtFileRegexActionPerformed(evt);
      }
    });

    jLabel2.setText("Decoy prefix");
    jLabel2.setToolTipText(txtDecoyPrefix.getToolTipText());

    txtDecoyPrefix.setText(StringUtils.join(getParamsDefault().getDecoyProtPrefix(), ","));
    txtDecoyPrefix.setToolTipText("Separate multiple entries with commas");

    panelCache.setBorder(javax.swing.BorderFactory.createTitledBorder("Files & Cache"));

    boxUseCache.setSelected(getParamsDefault().isUseCache());
    boxUseCache.setText("Use cache");
    boxUseCache.setToolTipText(
        "When selected, will store information parsed from pep.xml in separate files");
    boxUseCache.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        boxUseCacheActionPerformed(evt);
      }
    });

    jLabel3.setText("Cache file ext");
    jLabel3.setToolTipText(txtCacheExt.getToolTipText());

    txtCacheExt.setText(getParamsDefault().getCacheExt());
    txtCacheExt.setToolTipText("This extension will be used for cache files");

    boxForce.setText("Force Overwrite");
    boxForce.setToolTipText(
        "<html>If some file needs to be created but already exists, <br/>\nusing this option will force overwrite it. <br/>\nOtherwise the program will cowardly refuse to delete the existing file.");
    boxForce.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        boxForceActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout panelCacheLayout = new javax.swing.GroupLayout(panelCache);
    panelCache.setLayout(panelCacheLayout);
    panelCacheLayout.setHorizontalGroup(
        panelCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCacheLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(boxForce)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(boxUseCache)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCacheExt, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                    javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    panelCacheLayout.setVerticalGroup(
        panelCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(
                panelCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxUseCache)
                    .addComponent(jLabel3)
                    .addComponent(txtCacheExt, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxForce))
    );

    panelKDE.setBorder(javax.swing.BorderFactory.createTitledBorder("KDE"));

    boxMassDependent.setSelected(getParamsDefault().isDynamicBandwidthEstimation());
    boxMassDependent.setText("Auto Bandwidth");
    boxMassDependent.setToolTipText(
        "<html>Dynamic estimation of bandwidth at each nominal mass. <br/>\nWill only use points that are within the window range of the mass itself.");

    jLabel4.setText("Mass (h)");

    txtMassForBandwidthEstimate.setToolTipText(
        "<html>If non-empty value provided, bandwidth will be <br/>\nautomatically estimated using points that lie <br/>\nwithin \"Window(h)\" from this mass");

    jLabel5.setText("Window (h)");
    jLabel5.setToolTipText(spinnerMassWindowForBandwidthEstimate.getToolTipText());

    spinnerMassWindowForBandwidthEstimate.setModel(createSpinnerModel());
    spinnerMassWindowForBandwidthEstimate.setToolTipText(
        "<html>This window will be used if dynamic estimate of bandwidth <br/>\nwas requested by providing a non-empty value in \"Mass (h)\" box. <br/>\nIt will also be used by \"Mass dependent option\".");

    jLabel6.setText("Kernel type");
    jLabel6.setToolTipText(comboKernelType.getToolTipText());

    comboKernelType.setModel(createComboKernelType());
    comboKernelType.setToolTipText(
        "<html>Kernel function that will be used for KDE estimation. <br/>\nDifferences are in speed and accuracy. <br/>\nGaussian will be 100x slower than Epanechnikov, for example, <br/>\nbut will give smoother estimates.");

    jLabel7.setText("List of bandwidths");

    txtBandwidths.setText(createTextBandwidths());
    txtBandwidths.setToolTipText(
        "<html>You can manually provide a comma separated <br/>\nlist of bandwidths.<br/>\nE.g.: 0.01,0.001");

    jLabel8.setText("Score as weight");
    jLabel8.setToolTipText(txtWeightScoreName.getToolTipText());

    txtWeightScoreName.setToolTipText(
        "<html>One name of a corr, that must be present in all pepxml <br/>\nfiles that will be used as weighting factor in KDE analysis. <br/>\nIt only makes sense to use \"the larger - the better\" scores <br/>\nso don't use \"expect\" here.");

    jLabel12.setText("Mass Lo");
    jLabel12.setToolTipText(txtMassLo.getToolTipText());

    txtMassLo.setToolTipText(
        "<html>Low mass range end to use for KDE. <br/>\nLeave blank to use all mass differences. <br/>\nUse this option when you want to zoom in on a peak.");

    jLabel13.setText("-");

    txtMassHi.setToolTipText(
        "<html>High mass range end to use for KDE. <br/>\nLeave blank to use all mass differences. <br/>\nUse this option when you want to zoom in on a peak.");

    jLabel14.setText("Mass Hi");
    jLabel14.setToolTipText(txtMassHi.getToolTipText());

    jLabel15.setText("Mass Step");

    txtMassStep.setText(createTextMassStep());
    txtMassStep.setToolTipText("Mass step of the X axis for plotting");

    jLabel16.setText("Mass correction");
    jLabel16.setToolTipText(comboMassCorrection.getToolTipText());

    comboMassCorrection.setModel(createComboMassCorrection());
    comboMassCorrection.setToolTipText(
        "Right after loading data from files mass errors can be estimated and corrected");

    boxClearDmRange.setSelected(true);
    boxClearDmRange.setText("Clear range");
    boxClearDmRange.setToolTipText("<html>Remove masses in the range <br/>left of this checkbox");

    jLabel19.setText("dM Lo");

    spinnerClearDmLo.setModel(new javax.swing.SpinnerNumberModel(-0.5d, null, null, 0.5d));
    spinnerClearDmLo.setToolTipText("<html>Low end of removal mass range");

    jLabel20.setText("-");

    spinnerClearDmHi.setModel(new javax.swing.SpinnerNumberModel(0.5d, null, null, 0.5d));
    spinnerClearDmHi.setToolTipText("<html>Low end of removal mass range");

    jLabel21.setText("dM Hi");

    javax.swing.GroupLayout panelKDELayout = new javax.swing.GroupLayout(panelKDE);
    panelKDE.setLayout(panelKDELayout);
    panelKDELayout.setHorizontalGroup(
        panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelKDELayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(
                    panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelKDELayout.createSequentialGroup()
                            .addComponent(jLabel12)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtMassLo, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel13)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtMassHi, javax.swing.GroupLayout.PREFERRED_SIZE, 81,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel14)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel15)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtMassStep, javax.swing.GroupLayout.DEFAULT_SIZE, 104,
                                Short.MAX_VALUE))
                        .addGroup(panelKDELayout.createSequentialGroup()
                            .addComponent(jLabel16)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(comboMassCorrection,
                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(135, 135, 135)
                            .addComponent(jLabel8)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtWeightScoreName))
                        .addGroup(panelKDELayout.createSequentialGroup()
                            .addGroup(panelKDELayout
                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panelKDELayout.createSequentialGroup()
                                    .addComponent(boxMassDependent)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel4)
                                    .addGap(2, 2, 2)
                                    .addComponent(txtMassForBandwidthEstimate,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, 58,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel5)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(spinnerMassWindowForBandwidthEstimate,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, 70,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelKDELayout.createSequentialGroup()
                                    .addComponent(boxClearDmRange)
                                    .addGap(18, 18, 18)
                                    .addComponent(jLabel19)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(spinnerClearDmLo,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, 70,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel20)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(spinnerClearDmHi,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, 70,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel21)))
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(panelKDELayout.createSequentialGroup()
                            .addComponent(jLabel6)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(comboKernelType, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel7)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtBandwidths)))
                .addContainerGap())
    );
    panelKDELayout.setVerticalGroup(
        panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelKDELayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(
                    panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(boxClearDmRange)
                        .addComponent(jLabel19)
                        .addComponent(spinnerClearDmLo, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel20)
                        .addComponent(spinnerClearDmHi, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel21))
                .addGap(14, 14, 14)
                .addGroup(
                    panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(boxMassDependent)
                        .addComponent(jLabel4)
                        .addComponent(txtMassForBandwidthEstimate,
                            javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5)
                        .addComponent(spinnerMassWindowForBandwidthEstimate,
                            javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(
                    panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(comboKernelType, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7)
                        .addComponent(txtBandwidths, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(
                    panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel12)
                        .addComponent(txtMassLo, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel13)
                        .addComponent(txtMassHi, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel14)
                        .addComponent(jLabel15)
                        .addComponent(txtMassStep, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(
                    panelKDELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel16)
                        .addComponent(txtWeightScoreName, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8)
                        .addComponent(comboMassCorrection, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    boxDoPlot.setSelected(getParamsDefault().isPlotKde());
    boxDoPlot.setText("Plot");
    boxDoPlot.setToolTipText("Should the interactive plot be displayed?");

    panelOutput.setBorder(javax.swing.BorderFactory.createTitledBorder("Output"));
    panelOutput.setMinimumSize(new java.awt.Dimension(100, 100));

    jLabel9.setText("Output dir");
    jLabel9.setToolTipText(txtFileOut.getToolTipText());

    txtFileOut.setToolTipText("Directory where files with detected peaks will be written to");
    txtFileOut.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        txtFileOutActionPerformed(evt);
      }
    });

    btnFileOut.setText("Browse");
    btnFileOut.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFileOutActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout panelOutputLayout = new javax.swing.GroupLayout(panelOutput);
    panelOutput.setLayout(panelOutputLayout);
    panelOutputLayout.setHorizontalGroup(
        panelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtFileOut)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnFileOut))
    );
    panelOutputLayout.setVerticalGroup(
        panelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOutputLayout.createSequentialGroup()
                .addGroup(panelOutputLayout
                    .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtFileOut, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFileOut))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    jLabel10.setText("Input path");

    btnGo.setText("Go");
    btnGo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnGoActionPerformed(evt);
      }
    });

    panelFilters.setBorder(javax.swing.BorderFactory.createTitledBorder("Filters"));

    jLabel11.setText("Filter line");

    comboFilterScoreName.setModel(createComboFilterScoreName());
    comboFilterScoreName
        .setToolTipText("<html>Score name. <br/>\nYou can also manually edit the string below");
    comboFilterScoreName.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboFilterScoreNameActionPerformed(evt);
      }
    });

    comboFilterOperation.setModel(createComboComparisonType());
    comboFilterOperation.setToolTipText("Filter operation to be applied");

    btnFilterAdd.setText("Add");
    btnFilterAdd.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFilterAddActionPerformed(evt);
      }
    });

    btnFilterRemove.setText("Clear");
    btnFilterRemove.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnFilterRemoveActionPerformed(evt);
      }
    });

    txtFilterValue.setText("0.95");

    javax.swing.GroupLayout panelFiltersLayout = new javax.swing.GroupLayout(panelFilters);
    panelFilters.setLayout(panelFiltersLayout);
    panelFiltersLayout.setHorizontalGroup(
        panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFiltersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFiltersLayout
                    .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFiltersLayout.createSequentialGroup()
                        .addComponent(comboFilterScoreName, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboFilterOperation, javax.swing.GroupLayout.PREFERRED_SIZE,
                            javax.swing.GroupLayout.DEFAULT_SIZE,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFilterValue, javax.swing.GroupLayout.PREFERRED_SIZE, 70,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFilterAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFilterRemove)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(panelFiltersLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFilter)))
                .addContainerGap())
    );
    panelFiltersLayout.setVerticalGroup(
        panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFiltersLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelFiltersLayout
                    .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboFilterScoreName, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboFilterOperation, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFilterValue, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFilterAdd)
                    .addComponent(btnFilterRemove))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFiltersLayout
                    .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)))
    );

    btnConsoleClear.setText("Clear console");
    btnConsoleClear.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnConsoleClearActionPerformed(evt);
      }
    });

    panelPeakDetection.setBorder(javax.swing.BorderFactory.createTitledBorder("Peak Detection"));

    boxDetectPeaks.setSelected(getParamsDefault().isDetectPeaks());
    boxDetectPeaks.setText("Detect peaks");
    boxDetectPeaks.setToolTipText("Try to detect peaks in KDE results");

    btnGmmReset.setText("Defaults");
    btnGmmReset.setToolTipText("Reset parameters to defaults");
    btnGmmReset.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnGmmResetActionPerformed(evt);
      }
    });

    jLabel17.setText("Num pts");

    txtGmmNumPts.setText(createTextGmmNumPts());

    jLabel18.setText("Min pts");

    txtGmmMinPts.setText(createTextGmmMinPts());

    javax.swing.GroupLayout panelPeakDetectionLayout = new javax.swing.GroupLayout(
        panelPeakDetection);
    panelPeakDetection.setLayout(panelPeakDetectionLayout);
    panelPeakDetectionLayout.setHorizontalGroup(
        panelPeakDetectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPeakDetectionLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(boxDetectPeaks)
                .addGap(18, 18, 18)
                .addComponent(btnGmmReset)
                .addGap(18, 18, 18)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtGmmNumPts, javax.swing.GroupLayout.PREFERRED_SIZE, 53,
                    javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtGmmMinPts, javax.swing.GroupLayout.PREFERRED_SIZE, 58,
                    javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    panelPeakDetectionLayout.setVerticalGroup(
        panelPeakDetectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPeakDetectionLayout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(boxDetectPeaks)
                .addComponent(btnGmmReset)
                .addComponent(jLabel17)
                .addComponent(txtGmmNumPts, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel18)
                .addComponent(txtGmmMinPts, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );

    jLabel22.setText("Decoys treatment");

    comboDecoyTreatment.setModel(createComboDecoyTreatment());

    btnDeleteCache.setText("Clear Cache");
    btnDeleteCache.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnDeleteCacheActionPerformed(evt);
      }
    });

    textFilterFile.addFocusListener(new java.awt.event.FocusAdapter() {
      public void focusLost(java.awt.event.FocusEvent evt) {
        textFilterFileFocusLost(evt);
      }
    });

    jLabel23.setText("Filter File");

    btnBrowseFilterFile.setText("Browse");
    btnBrowseFilterFile.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnBrowseFilterFileActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelCache, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollPaneConsole, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(
                            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel23, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(
                                        javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtFileRegex,
                                            javax.swing.GroupLayout.DEFAULT_SIZE, 120,
                                            Short.MAX_VALUE)
                                        .addComponent(txtDecoyPrefix))
                                    .addGap(18, 18, 18)
                                    .addGroup(layout.createParallelGroup(
                                        javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(boxDoPlot)
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(jLabel22)
                                            .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(comboDecoyTreatment,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGap(0, 0, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                    layout.createSequentialGroup()
                                        .addComponent(textFilterFile)
                                        .addPreferredGap(
                                            javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnBrowseFilterFile))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(txtFileIn)
                                    .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnFileIn))))
                    .addComponent(panelKDE, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelFilters, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelPeakDetection, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelOutput, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnGo, javax.swing.GroupLayout.PREFERRED_SIZE, 57,
                            javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnConsoleClear)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                            javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDeleteCache)))
                .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFileIn, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFileIn)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtFileRegex, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxDoPlot))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtDecoyPrefix, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22)
                    .addComponent(comboDecoyTreatment, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFilterFile, javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23)
                    .addComponent(btnBrowseFilterFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelCache, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelKDE, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelFilters, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelPeakDetection, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelOutput, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGo)
                    .addComponent(btnConsoleClear)
                    .addComponent(btnDeleteCache))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPaneConsole, javax.swing.GroupLayout.DEFAULT_SIZE, 174,
                    Short.MAX_VALUE)
                .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void txtFileRegexActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFileRegexActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_txtFileRegexActionPerformed

  private void boxForceActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxForceActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_boxForceActionPerformed

  private void btnFileInActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFileInActionPerformed
    if (btnFileIn == evt.getSource()) {
      String approveText = "Select";
      JFileChooser fc = new JFileChooser();
      fc.setAcceptAllFileFilterUsed(true);
      fc.addChoosableFileFilter(new FileFilter() {
        @Override
        public boolean accept(File f) {
          String name = f.getName().toLowerCase();
          return name.endsWith(".pep.xml") || name.endsWith(".pepxml");
        }

        @Override
        public String getDescription() {
          return "PepXML files";
        }
      });
      fc.setApproveButtonText(approveText);
      fc.setDialogTitle("Choose input directory or files");
      fc.setMultiSelectionEnabled(true);
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

      List<String> filePaths = splitTrim(txtFileIn.getText().trim(), ",");
      for (int i = 0; i < filePaths.size(); i++) {
        String filePath = filePaths.get(i);
        Path p = Paths.get(filePath).toAbsolutePath();
        if (Files.exists(p)) {
          fc.setSelectedFile(p.toFile());
        }
      }

      int filechooserResult = fc.showDialog(this, approveText);
      if (JFileChooser.APPROVE_OPTION == filechooserResult) {
        File[] files = fc.getSelectedFiles();
        String filesStr = StringUtils.join(Arrays.asList(files), ", ");
        //This is where a real application would open the file.
        log.debug("User selected files: {}", filesStr);
        setPathIn(filesStr);
        if (files.length > 0) {
          KDEGuiProps.save(KDEGuiProps.PROP_FILE_IN, filesStr);
          txtFileIn.setText(filesStr);
          if (files.length == 1 &&
              Files.exists(files[0].toPath()) &&
              Files.isDirectory(files[0].toPath())) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Input is a directory.\n"
                    + "Do you want to set the output to the same location?");
            if (JOptionPane.OK_OPTION == choice) {
              txtFileOut.setText(files[0].toString());
            }
          }
        }

      } else {
        log.debug("Choose input files command cancelled by user.");
      }
    }
  }//GEN-LAST:event_btnFileInActionPerformed

  private void btnGoActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGoActionPerformed
    run();
  }//GEN-LAST:event_btnGoActionPerformed

  private void comboFilterScoreNameActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboFilterScoreNameActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_comboFilterScoreNameActionPerformed

  private void btnFilterRemoveActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterRemoveActionPerformed
    txtFilter.setText("");
  }//GEN-LAST:event_btnFilterRemoveActionPerformed

  private void btnFilterAddActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterAddActionPerformed
    String text = txtFilterValue.getText();
    if (text.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Filter value can't be empty", STR_PARAM_ERROR,
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    try {
      double value = Double.parseDouble(text);
    } catch (NumberFormatException nfe) {
      JOptionPane.showMessageDialog(this,
          String.format("Not a number in filter value.\n%s", nfe.getMessage()), STR_PARAM_ERROR,
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    String filterLine = txtFilter.getText();
    StringBuilder sb = new StringBuilder(filterLine.trim());
    if (filterLine.length() > 0) {
      sb.append(",");
    }
    sb.append(comboFilterScoreName.getSelectedItem().toString());
    sb.append(comboFilterOperation.getSelectedItem().toString());
    sb.append(text);
    txtFilter.setText(sb.toString());
  }//GEN-LAST:event_btnFilterAddActionPerformed

  private void txtFileInActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFileInActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_txtFileInActionPerformed

  private void btnConsoleClearActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConsoleClearActionPerformed
    console.setText("");
  }//GEN-LAST:event_btnConsoleClearActionPerformed

  private void btnFileOutActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFileOutActionPerformed
    if (btnFileOut == evt.getSource()) {
      String approveText = "Select";
      JFileChooser fc = new JFileChooser();
      fc.setAcceptAllFileFilterUsed(true);
      fc.setApproveButtonText(approveText);
      fc.setDialogTitle("Choose output path");
      fc.setMultiSelectionEnabled(false);
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      List<String> filePaths = splitTrim(txtFileOut.getText().trim(), ",");
      if (!filePaths.isEmpty()) {
        for (int i = 0; i < filePaths.size(); i++) {
          String filePath = filePaths.get(i);
          Path p = Paths.get(filePath).toAbsolutePath();
          if (Files.exists(p)) {
            fc.setSelectedFile(p.toFile());
            break;
          }
        }
      } else {
        // try to set it to the path of input file
        filePaths = splitTrim(txtFileIn.getText().trim(), ",");
        if (!filePaths.isEmpty()) {
          for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            Path p = Paths.get(filePath).toAbsolutePath();
            if (Files.exists(p)) {
              fc.setSelectedFile(p.toFile());
              break;
            }
          }
        }
      }

      int retVal = fc.showDialog(this, approveText);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        if (file == null) {
          return;
        }
        String pathStr = file.getAbsolutePath();
        txtFileOut.setText(pathStr);
        //This is where a real application would open the file.
        log.debug("User selected output dir: {}", pathStr);
        setPathOut(pathStr);
        KDEGuiProps props = KDEGuiProps.loadFromTemp();
        props = props == null ? new KDEGuiProps() : props;
        props.put(KDEGuiProps.PROP_FILE_OUT, pathStr);
        props.save();
      } else {
        log.debug("Choose output path command cancelled by user.");
      }
    }
  }//GEN-LAST:event_btnFileOutActionPerformed

  private void txtFileOutActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFileOutActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_txtFileOutActionPerformed

  private void btnGmmResetActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGmmResetActionPerformed
    txtGmmNumPts.setText(createTextGmmNumPts());
    txtGmmMinPts.setText(createTextGmmMinPts());
  }//GEN-LAST:event_btnGmmResetActionPerformed

  private void boxUseCacheActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxUseCacheActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_boxUseCacheActionPerformed

  private void btnDeleteCacheActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteCacheActionPerformed

    String text = txtFileIn.getText();
    if (StringUtils.isBlank(text)) {
      JOptionPane.showConfirmDialog(this, "Invalid input path", "Warning",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Are you sure you want to delete cached input files?\n");
    sb.append("They will need to be re-parsed at next run.");

    int choice = JOptionPane
        .showConfirmDialog(this, sb.toString(), "Clear cache", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
    if (JOptionPane.OK_OPTION == choice) {
      final OpenSearchParams params = new OpenSearchParams();

      List<Path> inFilePath = new ArrayList<>();
      List<String> filePaths = splitTrim(text, ",");
      for (int i = 0; i < filePaths.size(); i++) {
        String filePath = filePaths.get(i);
        Path p = Paths.get(filePath).toAbsolutePath();
        if (!Files.exists(p)) {
          JOptionPane.showMessageDialog(this, String.format(
              "Not an existing path: %s", p.toString()), STR_PARAM_ERROR,
              JOptionPane.WARNING_MESSAGE);
          return;
        }
        inFilePath.add(p);
      }
      params.setInFilePath(inFilePath);

      params.setDeleteCache(true);
      final KDEMain kdeMain = new KDEMain();

      kdeMain.setChartCloseOption(JFreeChartPlot.ChartCloseOption.DISPOSE);

      Runnable runnable = () -> kdeMain.run(params);
      REHandler reHandler = new REHandler(runnable, System.err, console);
      exec.execute(reHandler);
    }
  }//GEN-LAST:event_btnDeleteCacheActionPerformed

  private void textFilterFileFocusLost(
      java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textFilterFileFocusLost
    saveTextFilterFile(null);
  }//GEN-LAST:event_textFilterFileFocusLost

  private void btnBrowseFilterFileActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseFilterFileActionPerformed
    String approveText = "Select";
    JFileChooser fc = new JFileChooser();
    fc.setAcceptAllFileFilterUsed(true);
    fc.setApproveButtonText(approveText);
    fc.setDialogTitle("Choose filter file");
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

    String text = textFilterFile.getText();
    try {
      Path p = Paths.get(text);
      if (Files.exists(p)) {
        fc.setSelectedFile(p.toFile());
      } else {
        KDEGuiProps props = KDEGuiProps.loadFromTemp();
        String filterFile = props.getProperty(KDEGuiProps.PROP_FILE_FILTER);
        if (filterFile != null) {
          p = Paths.get(filterFile);
          if (Files.exists(p)) {
            fc.setSelectedFile(p.toFile());
          }
        }
      }
    } catch (Exception e) {
      // novermind
    }

    int result = fc.showDialog(this, approveText);
    if (JFileChooser.APPROVE_OPTION == result) {
      String file = fc.getSelectedFile().getAbsolutePath();
      saveTextFilterFile(file);
    } else {
      log.debug("Choose input files command cancelled by user.");
    }
  }//GEN-LAST:event_btnBrowseFilterFileActionPerformed

  private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
    if (StringUtils.isBlank(txtFileIn.getText())) {
      String previousPathsIn = getPreviousPathsIn();
      txtFileIn.setText(previousPathsIn);
    }
  }//GEN-LAST:event_formWindowOpened

  /**
   * @param toSave if null, value from the text field will be taken.
   */
  private void saveTextFilterFile(String toSave) {
    String t = toSave != null ? toSave : textFilterFile.getText();
    if (StringUtils.isBlank(t)) {
      return;
    }
    try {
      Path p = Paths.get(t);
      if (!Files.exists(p)) {
        return;
      }
      String f = p.toAbsolutePath().toString();
      KDEGuiProps.save(KDEGuiProps.PROP_FILE_FILTER, f);
      textFilterFile.setText(f);
    } catch (Exception e) {
      // nevermind
    }
  }

  public static class REHandler implements Runnable {

    Runnable delegate;
    Appendable[] outs;

    public REHandler(Runnable delegate, Appendable... out) {
      this.delegate = delegate;
      this.outs = out;
    }

    public void run() {
      try {
        delegate.run();
      } catch (Exception e) {
        log.error("Something bad happened in a worker thread", e);
        String msg = String
            .format("Something bad happened in a worker thread:\n%s", e.getMessage());
        for (Appendable out : outs) {
          LogUtils.println(out, msg);
        }
      }
    }
  }
  // End of variables declaration//GEN-END:variables


}
