package umich.opensearch.kde.jfree;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/**
 * @author Dmitry Avtonomov
 */
public class TextConsole extends JTextPane implements Appendable {

  @Override
  public Appendable append(CharSequence csq) {
    //append(csq.toString());
    StyledDocument doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), csq.toString(), null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) {
    //append(csq.subSequence(start, end).toString());
    StyledDocument doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), csq.subSequence(start, end).toString(), null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
    return this;
  }

  @Override
  public Appendable append(char c) {
    //append(Character.toString(c));
    StyledDocument doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), String.valueOf(c), null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
    return this;
  }
}
