package com.dmtavt.deltamass.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;

public class ArrayUtils {
  private ArrayUtils() {}

  public static void main(String[] args) {

  }

  /**
   * Removes zero valued entries from arrays in a given list, based on values in the first
   * array.
   * @param arrays Values from the first array in this list are used.
   * @param leaveFlanking Whether to leave flanking points (that don't match the criteria) around
   * each accepted value. Useful when you want to keep zero entries around non-zero values for
   * plotting.
   * @return A new list with new filtered data arrays.
   */
  public static List<double[]> removeZeros(List<double[]> arrays, boolean leaveFlanking) {
    return remove(arrays, value -> value != 0, leaveFlanking);
  }

  /**
   * Removes values from arrays in a given list that don't match a predicate. Values from the first
   * array are used to compute the set of indicies to be left, the same entries are left for the
   * other arrays in the list.
   * @param arrays Values from the first array in this list are used.
   * @param predicate The criterion for leaving the entries in.
   * @param leaveFlanking Whether to leave flanking points (that don't match the criteria) around
   * each accepted value. Useful when you want to keep zero entries around non-zero values for
   * plotting.
   * @return A new list with new filtered data arrays. An empty list if input list is null or empty.
   */
  public static List<double[]> remove(List<double[]> arrays, DoublePredicate predicate, boolean leaveFlanking) {
    if (arrays == null || arrays.isEmpty())
      return Collections.emptyList();
    Set<Integer> lengths = arrays.stream().map(arr -> arr.length)
        .collect(Collectors.toSet());
    if (lengths.size() != 1)
      throw new IllegalArgumentException("All arrays must be of the same length");

    final double[] base = arrays.get(0);
    int[] accepted = new int[base.length];
    int ptr = -1;
    boolean inside = false;
    // first element
    if (predicate.test(base[0])) {
      accepted[++ptr] = 0;
      inside = true;
    }
    for (int i = 1, cap = base.length; i < cap; i++) {
      if (predicate.test(base[i])) {
        if (leaveFlanking && !inside) {
          // transition to non-zero region
          inside = true;
          accepted[++ptr] = i - 1;
        }
        accepted[++ptr] = i;
      } else {
        if (leaveFlanking && inside) {
          // transition to zero region
          inside = false;
          accepted[++ptr] = i;
        }
      }
    }

    final int acceptedSize = ptr + 1;

    List<double[]> result = new ArrayList<>(arrays.size());
    for (double[] arr : arrays) {
      double[] filtered = new double[acceptedSize];
      for (int i = 0; i < acceptedSize; i++) {
        filtered[i] = arr[accepted[i]];
      }
      result.add(filtered);
    }

    return result;
  }
}
