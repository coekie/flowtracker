package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FlowTesterTest {
  private final FlowTester ft = new FlowTester();
  private final FlowTester ft2 = new FlowTester();

  @Test
  public void testCreateAndAssertTrackedValue_char() {
    char a = ft.createSourceChar('a');
    FlowTester.assertTrackedValue(a, 'a', ft.tracker(), ft.index());
  }

  @Test
  public void testCreateAndAssertTrackedValue_byte() {
    byte a = ft.createSourceByte((byte) 'a');
    FlowTester.assertTrackedValue(a, (byte) 'a', ft.tracker(), ft.index());
  }

  @Test
  public void testCreateAndAssertIsTheTrackedValue_char() {
    char a = ft.createSourceChar('a');
    char b = ft2.createSourceChar('b');
    ft.assertIsTheTrackedValue(a);
    ft2.assertIsTheTrackedValue(b);

    try {
      ft.assertIsTheTrackedValue(b);
      throw new RuntimeException("failed");
    } catch (AssertionError expected) {
    }
  }

  @Test
  public void testCreateAndAssertIsTheTrackedValue_byte() {
    byte a = ft.createSourceByte((byte) 'a');
    byte b = ft2.createSourceByte((byte) 'b');
    ft.assertIsTheTrackedValue(a);
    ft2.assertIsTheTrackedValue(b);

    try {
      ft.assertIsTheTrackedValue(b);
      throw new RuntimeException("failed");
    } catch (AssertionError expected) {
    }
  }

  @Test
  public void testCreateAndAssertIsTheTrackedValue_int() {
    int a = ft.createSourceInt('a');
    int b = ft2.createSourceInt('b');
    ft.assertIsTheTrackedValue((char) a);
    ft2.assertIsTheTrackedValue((char) b);

    try {
      ft.assertIsTheTrackedValue((char) b);
      throw new RuntimeException("failed");
    } catch (AssertionError expected) {
    }
  }

  @Test
  public void testCreateAndAssertIsTheTrackedValue_long() {
    long a = ft.createSourceLong('a');
    long b = ft2.createSourceLong('b');
    ft.assertIsTheTrackedValue(a);
    ft2.assertIsTheTrackedValue(b);

    try {
      ft.assertIsTheTrackedValue(b);
      throw new RuntimeException("failed");
    } catch (AssertionError expected) {
    }
  }

  @Test
  public void testGetCharSourcePoint() {
    char a = ft.createSourceChar('a');
    assertThat(FlowTester.getCharSourcePoint(a)).isEqualTo(ft.point());
  }

  @Test
  public void testGetByteSourcePoint() {
    byte a = ft.createSourceByte((byte) 'a');
    assertThat(FlowTester.getByteSourcePoint(a)).isEqualTo(ft.point());
  }

  @Test
  public void testGetTracker_int() {
    int a = ft.createSourceInt((byte) 'a');
    assertThat(FlowTester.getIntSourcePoint(a)).isEqualTo(ft.point());
  }
}
