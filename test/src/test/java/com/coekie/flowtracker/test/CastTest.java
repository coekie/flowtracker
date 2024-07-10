package com.coekie.flowtracker.test;

import org.junit.Test;

/** Tests involving CastValue */
public class CastTest {
  @Test
  public void cast() {
    FlowTester ft = new FlowTester();
    int i = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((byte) i);
    ft.assertIsTheTrackedValue((char) i);
    ft.assertIsTheTrackedValue((byte) (int) (byte) i);
    ft.assertIsTheTrackedValue((char) (int) (char) i);
    ft.assertIsTheTrackedValue((char) (short) (char) i);
  }

  // casts with long are special because long takes two slots.
  @Test public void castFromLong() {
    FlowTester ft = new FlowTester();
    long i = ft.createSourceLong('a');
    ft.assertIsTheTrackedValue((int) i);
    ft.assertIsTheTrackedValue((byte) i);
    ft.assertIsTheTrackedValue((char) i);
    ft.assertIsTheTrackedValue((byte) (int) (byte) i);
    ft.assertIsTheTrackedValue((char) (int) (char) i);
    ft.assertIsTheTrackedValue((char) (short) (char) i);
  }

  // casts with long are special because long takes two slots.
  @SuppressWarnings("RedundantCast")
  @Test public void castToLong() {
    FlowTester ft = new FlowTester();
    int i = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((long) i);
    ft.assertIsTheTrackedValue((long) (char) i);
    ft.assertIsTheTrackedValue((long) (byte) i);
    ft.assertIsTheTrackedValue((long) (short) i);
  }

}
