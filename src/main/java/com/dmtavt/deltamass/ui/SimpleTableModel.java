package com.dmtavt.deltamass.ui;

import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

public class SimpleTableModel<FROM> extends AbstractTableModel {
  List<FROM> data;
  List<Col<FROM, ? extends Object>> cols;

  public static class Col<FROM, TO> {
    final Class<TO> clazz;
    final Function<FROM, TO> valueFetcher;
    final String colName;

    public Col(Class<TO> clazz, Function<FROM, TO> valueFetcher, String colName) {
      this.clazz = clazz;
      this.valueFetcher = valueFetcher;
      this.colName = colName;
    }
  }

  public static class ColDouble<T> extends Col<T, Double> {
    public ColDouble(Function<T, Double> valueFetcher, String colName) {
      super(Double.class, valueFetcher, colName);
    }
  }
  public static class ColInt<T> extends Col<T, Integer> {
    public ColInt(Function<T, Integer> valueFetcher, String colName) {
      super(Integer.class, valueFetcher, colName);
    }
  }
  public static class ColBool<T> extends Col<T, Boolean> {
    public ColBool(Function<T, Boolean> valueFetcher, String colName) {
      super(Boolean.class, valueFetcher, colName);
    }
  }
  public static class ColString<T> extends Col<T, String> {
    public ColString(Function<T, String> valueFetcher, String colName) {
      super(String.class, valueFetcher, colName);
    }
  }

  public SimpleTableModel(List<FROM> data,
      List<Col<FROM, ? extends Object>> cols) {
    this.data = data;
    this.cols = cols;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return cols.get(columnIndex).clazz;
  }

  @Override
  public int getRowCount() {
    return data.size();
  }

  @Override
  public int getColumnCount() {
    return cols.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return cols.get(columnIndex).valueFetcher.apply(data.get(rowIndex));
  }

  @Override
  public String getColumnName(int column) {
    return cols.get(column).colName;
  }
}
