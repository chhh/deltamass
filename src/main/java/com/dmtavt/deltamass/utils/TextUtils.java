package com.dmtavt.deltamass.utils;

public final class TextUtils {

  private TextUtils() {
  }

  /**
   * Wrap text to fixed column size.
   *
   * @param out Output will be placed here.
   * @param colSize Length to wrap to.
   * @param indent Starting indentation of text.
   * @param text Text to wrap.
   */
  public static void wrap(StringBuilder out, int colSize, int indent, String text) {
    String[] words = text.split(" ");
    int current = 0;
    int i = 0;
    while (i < words.length) {
      String word = words[i];
      if (word.length() > colSize || current + 1 + word.length() <= colSize) {
        out.append(word);
        current += word.length();
        if (i != words.length - 1) {
          out.append(" ");
          current++;
        }
      } else {
        out.append("\n").append(repeat(" ", indent)).append(word).append(" ");
        current = indent + 1 + word.length();
      }
      i++;
    }
  }

  /**
   * Create a string of N spaces.
   *
   * @param count number of space characters.
   * @return A new string with spaces.
   */
  public static String repeat(String str, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append(str);
    }

    return sb.toString();
  }
}
