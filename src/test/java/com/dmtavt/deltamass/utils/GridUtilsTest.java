package com.dmtavt.deltamass.utils;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

public class GridUtilsTest {

  @Test
  public void gridTest1() {
    double lo = -1.0;
    double hi = 1.0;
    double step = 0.1;
    double eps = 1e-8;

    double[] grid = GridUtils.grid(lo, hi, step);
    assertEquals(21, grid.length);
    assertEquals(lo, grid[0], eps);
    assertEquals(hi, grid[grid.length-1], eps);
  }

  @Test
  public void gridTest2() {
    double lo = -1.05;
    double hi = 1.05;
    double step = 0.1;
    double eps = 1e-8;

    double[] grid = GridUtils.grid(lo, hi, step);
    assertEquals(22, grid.length);
    assertEquals(lo, grid[0], eps);
    assertEquals(hi, grid[grid.length-1], eps);
  }

  @Test
  public void gridTest3() {
    double lo = -1.1;
    double hi = 1.05;
    double step = 0.1;
    double eps = 1e-8;

    double[] grid = GridUtils.grid(lo, hi, step);
    assertEquals(23, grid.length);
    assertEquals(lo, grid[0], eps);
    assertTrue(grid[grid.length-1] > hi);
    assertTrue(Math.abs(grid[grid.length-1] - hi) < step);
  }

  @Test
  public void gridWithPivotTest1() {
    double lo = -1.0;
    double hi = 1.0;
    double pivot = 0.00;
    double step = 0.2;
    double margin = 0.0;
    double eps = 1e-8;

    double[] grid = GridUtils.grid(pivot, lo, hi, step, margin);
    assertEquals(11, grid.length);
    assertEquals(lo, grid[0], eps);
    assertEquals(hi, grid[grid.length - 1], eps);
  }

  @Test
  public void gridWithPivotTest2() {
    double lo = -1.0;
    double hi = 1.0;
    double pivot = 0.05;
    double step = 0.2;
    double margin = 0.0;
    double eps = 1e-8;

    double[] grid = GridUtils.grid(pivot, lo, hi, step, margin);
    assertEquals(12, grid.length);
    assertTrue(Arrays.stream(grid).filter(v -> Math.abs(v - pivot) <= eps).findAny().isPresent());
    assertTrue(grid[0] <= lo);
    assertTrue(grid[grid.length-1] >= hi);
  }

  @Test
  public void gridWithPivotTest3() {
    double lo = -1.0;
    double hi = 1.0;
    double pivot = 0.05;
    double step = 0.2;
    double margin = 0.2;
    double eps = 1e-8;

    double[] grid = GridUtils.grid(pivot, lo, hi, step, margin);
    assertEquals(14, grid.length);
    assertTrue(Arrays.stream(grid).filter(v -> Math.abs(v - pivot) <= eps).findAny().isPresent());
    assertTrue(grid[0] <= lo);
    assertTrue(grid[grid.length-1] >= hi);
  }
}
