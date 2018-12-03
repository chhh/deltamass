package com.dmtavt.deltamass.utils;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ArrayUtilsTest {
  private List<double[]> in;
  double[] a0, a1;

  @Before
  public void setup() {
    setup(5);
  }

  private void setup(int arraySize) {
    in = new ArrayList<>();
    a0 = new double[arraySize];
    a1 = new double[arraySize];
    in.add(a0);
    in.add(a1);
  }

  @Test
  public void differentArraySizes() {
    in = new ArrayList<>();
    in.add(new double[1]);
    in.add(new double[2]);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ArrayUtils.removeZeros(in, true));
  }

  @Test
  public void onlyZerosIn() {
    setup(100);
    List<double[]> doubles = ArrayUtils.removeZeros(in, true);
    for (double[] arr : doubles) {
      assertThat(arr.length).isEqualTo(0);
    }
  }

  @Test
  public void singleNonZeroMidNoFlanking() {
    a0[2] = 1;
    a1[2] = 77;
    List<double[]> out = ArrayUtils.removeZeros(in, false);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(1);
    }
    assertThat(out.get(0)[0]).isEqualTo(1);
    assertThat(out.get(1)[0]).isEqualTo(77);
  }

  @Test
  public void singleNonZeroLeftNoFlanking() {
    a0[0] = 1;
    a1[0] = 77;
    List<double[]> out = ArrayUtils.removeZeros(in, false);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(1);
    }
    assertThat(out.get(0)[0]).isEqualTo(1);
    assertThat(out.get(1)[0]).isEqualTo(77);
  }

  @Test
  public void singleNonZeroRightNoFlanking() {
    a0[4] = 1;
    a1[4] = 77;
    List<double[]> out = ArrayUtils.removeZeros(in, false);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(1);
    }
    assertThat(out.get(0)[0]).isEqualTo(1);
    assertThat(out.get(1)[0]).isEqualTo(77);
  }

  @Test
  public void singleNonZeroMidFlanking() {
    a0[2] = 1;
    a1[2] = 77;
    List<double[]> out = ArrayUtils.removeZeros(in, true);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(3);
    }
    assertThat(out.get(0)[0]).isEqualTo(0);
    assertThat(out.get(0)[1]).isEqualTo(1);
    assertThat(out.get(0)[2]).isEqualTo(0);
    assertThat(out.get(1)[0]).isEqualTo(0);
    assertThat(out.get(1)[1]).isEqualTo(77);
    assertThat(out.get(1)[2]).isEqualTo(0);
  }

  @Test
  public void singleNonZeroLeftFlanking() {
    a0[0] = 1;
    a1[0] = 77;
    List<double[]> out = ArrayUtils.removeZeros(in, true);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(2);
    }
    assertThat(out.get(0)[0]).isEqualTo(1);
    assertThat(out.get(0)[1]).isEqualTo(0);
    assertThat(out.get(1)[0]).isEqualTo(77);
    assertThat(out.get(1)[1]).isEqualTo(0);
  }

  @Test
  public void singleNonZeroRightFlanking() {
    a0[4] = 1;
    a1[4] = 77;
    List<double[]> out = ArrayUtils.removeZeros(in, true);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(2);
    }
    assertThat(out.get(0)[0]).isEqualTo(0);
    assertThat(out.get(0)[1]).isEqualTo(1);
    assertThat(out.get(1)[0]).isEqualTo(0);
    assertThat(out.get(1)[1]).isEqualTo(77);
  }

  @Test
  public void twoNonZeroMidFlanking() {
    a0[2] = 1;
    a0[3] = 2;
    a1[2] = 77;
    a1[3] = 78;
    List<double[]> out = ArrayUtils.removeZeros(in, true);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(4);
    }
    assertThat(out.get(0)[0]).isEqualTo(0);
    assertThat(out.get(0)[1]).isEqualTo(1);
    assertThat(out.get(0)[2]).isEqualTo(2);
    assertThat(out.get(0)[3]).isEqualTo(0);
    assertThat(out.get(1)[0]).isEqualTo(0);
    assertThat(out.get(1)[1]).isEqualTo(77);
    assertThat(out.get(1)[2]).isEqualTo(78);
    assertThat(out.get(1)[3]).isEqualTo(0);
  }

  @Test
  public void twoNonZeroLeftFlanking() {
    a0[0] = 1;
    a0[1] = 2;
    a1[0] = 77;
    a1[1] = 78;
    List<double[]> out = ArrayUtils.removeZeros(in, true);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(3);
    }
    assertThat(out.get(0)[0]).isEqualTo(1);
    assertThat(out.get(0)[1]).isEqualTo(2);
    assertThat(out.get(0)[2]).isEqualTo(0);
    assertThat(out.get(1)[0]).isEqualTo(77);
    assertThat(out.get(1)[1]).isEqualTo(78);
    assertThat(out.get(1)[2]).isEqualTo(0);
  }

  @Test
  public void twoNonZeroRightFlanking() {
    a0[3] = 1;
    a0[4] = 2;
    a1[3] = 77;
    a1[4] = 78;
    List<double[]> out = ArrayUtils.removeZeros(in, true);
    for (double[] arr : out) {
      assertThat(arr.length).isEqualTo(3);
    }
    assertThat(out.get(0)[0]).isEqualTo(0);
    assertThat(out.get(0)[1]).isEqualTo(1);
    assertThat(out.get(0)[2]).isEqualTo(2);
    assertThat(out.get(1)[0]).isEqualTo(0);
    assertThat(out.get(1)[1]).isEqualTo(77);
    assertThat(out.get(1)[2]).isEqualTo(78);
  }
}
