package umich.opensearch.kde.jfree;

import java.text.NumberFormat;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

/**
 * @author Dmitry Avtonomov
 */
public class InfoWindow extends JFrame {

  private static final int WINDOW_SIZE_PRECISION = 4;
  public JPanel panelMain;
  public JPanel panelControls;
  public JFormattedTextField txtWndSize;
  public JFormattedTextField txtEntriesToShow;
  public TextConsole textConsole;
  public double listenerWindowSize = 0.01;
  public int entriesToShow = 200;
  JFrame parent = null;

  public InfoWindow(String title, JFrame parent) throws HeadlessException {
    super(title);
    this.parent = parent;
    if (parent != null) {
      this.setIconImages(parent.getIconImages());
    }
    init();
  }

  public TextConsole getTextConsole() {
    return textConsole;
  }

  @Override
  public JFrame getParent() {
    return parent;
  }

  public double getListenerWindowSize() {
    return listenerWindowSize;
  }

  public void setListenerWindowSize(double listenerWindowSize) {
    this.listenerWindowSize = listenerWindowSize;
  }

  public int getEntriesToShow() {
    return entriesToShow;
  }

  public void setEntriesToShow(int entriesToShow) {
    this.entriesToShow = entriesToShow;
  }

  private void init() {
    setPreferredSize(new Dimension(640, 720));

    panelMain = new JPanel();
    panelMain.setLayout(new BorderLayout());
    setContentPane(panelMain);

    FlowLayout flowLayout = new FlowLayout();
    flowLayout.setAlignment(FlowLayout.LEFT);
    flowLayout.setAlignOnBaseline(true);

    panelControls = new JPanel(flowLayout);
    panelMain.add(panelControls, BorderLayout.NORTH);

    panelControls.add(new JLabel("Window Size"));
    NumberFormat fmt = NumberFormat.getInstance();
    fmt.setMaximumFractionDigits(WINDOW_SIZE_PRECISION);
    txtWndSize = new JFormattedTextField(fmt);
    txtWndSize.setValue(getListenerWindowSize());
    txtWndSize.setPreferredSize(new Dimension(50, 25));
    txtWndSize.getDocument().addDocumentListener(new TxtWndSizeDocListener());
    panelControls.add(txtWndSize);

    panelControls.add(new JSeparator());
    panelControls.add(new JLabel("Entries to show"));
    NumberFormat fmt2 = NumberFormat.getInstance();
    fmt2.setMaximumFractionDigits(0);
    txtEntriesToShow = new JFormattedTextField(fmt2);
    txtEntriesToShow.setValue(getEntriesToShow());
    txtEntriesToShow.setPreferredSize(new Dimension(50, 25));
    txtEntriesToShow.getDocument().addDocumentListener(new TxtEntriesDocListener());
    panelControls.add(txtEntriesToShow);

    textConsole = new TextConsole();
    JScrollPane scroll = new JScrollPane(textConsole);
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        textConsole.setLineWrap(false);
    Font font = textConsole.getFont().deriveFont(12f);
    textConsole.setFont(font);
    textConsole.setVisible(true);
    DefaultCaret caret = (DefaultCaret) textConsole.getCaret();
    caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

    panelMain.add(scroll, BorderLayout.CENTER);
    if (parent.isVisible()) {
      setLocation(parent.getX() + parent.getWidth(), parent.getY());
    }

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    pack();
  }

  private class TxtWndSizeDocListener implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
      try {
        double val = Double.parseDouble(txtWndSize.getText());
        setListenerWindowSize(val);
      } catch (NumberFormatException nfe) {
      }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      try {
        double val = Double.parseDouble(txtWndSize.getText());
        setListenerWindowSize(val);
      } catch (NumberFormatException nfe) {
      }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }
  }

  private class TxtEntriesDocListener implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
      try {
        int val = Integer.parseInt(txtEntriesToShow.getText());
        setEntriesToShow(val);
      } catch (NumberFormatException nfe) {
      }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      try {
        int val = Integer.parseInt(txtEntriesToShow.getText());
        setEntriesToShow(val);
      } catch (NumberFormatException nfe) {
      }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }
  }

}
