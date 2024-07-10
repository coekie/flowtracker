package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.UnsafeHook;
import org.junit.Test;
import sun.misc.Unsafe;

public class UnsafeTest {
  private static final Unsafe unsafe = UnsafeHook.unsafe;
  private final FlowTester ft = new FlowTester();

  @Test
  public void putByte() {
    byte[] array = new byte[3];
    unsafe.putByte(array, unsafe.arrayBaseOffset(byte[].class) + 1,
        ft.createSourceByte((byte) 'a'));
    assertThat(array[1]).isEqualTo('a');
    assertThatTrackerOf(array).matches(
        snapshot().gap(1).part(ft.point()));
  }
}
