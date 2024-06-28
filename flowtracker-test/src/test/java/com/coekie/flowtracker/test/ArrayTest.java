package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;
import static com.coekie.flowtracker.test.TrackTestHelper.trackedIntArray;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.coekie.flowtracker.tracker.TrackerRepository.getTracker;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.TrackerSnapshot;
import org.junit.Test;

/**
 * Test for loading values from and storing values in arrays, and other array operations.
 *
 * @see ArraysTest
 */
public class ArrayTest {
  private final FlowTester ft = new FlowTester();

  @Test
  public void charArrayLoadValue() {
    char[] array = TrackTestHelper.trackedCharArrayWithLength(3);
    FlowTester.assertTrackedValue(array[2], '\0', getTracker(context(), array), 2);
  }

  @Test public void charArrayStore() {
    char[] array = new char[3];
    array[2] = ft.createSourceChar('a');
    assertThatTrackerOf(array).matches(
        snapshot().gap(2).part(ft.point()));
  }

  @Test public void charArrayLoadAndStore() {
    char[] abc = TrackTestHelper.trackedCharArrayWithLength(3);

    char[] array = new char[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    assertThatTrackerOf(array).matches(
        snapshot().track(1, abc, 1).track(1, abc, 0).track(1, abc, 2));
  }

  @Test public void byteArrayLoadAndStore() {
    byte[] abc = trackedByteArray("abc");

    byte[] array = new byte[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    assertThatTrackerOf(array).matches(
        snapshot().track(1, abc, 1).track(1, abc, 0).track(1, abc, 2));
  }

  @Test public void charArrayClone() {
    char[] array = TrackTestHelper.trackedCharArrayWithLength(3);
    assertThatTrackerOf(array.clone()).matches(snapshot().track(array));
  }

  @Test public void byteArrayClone() {
    byte[] array = trackedByteArray("abc");
    assertThatTrackerOf(array.clone()).matches(snapshot().track(array));
  }

  @Test public void intArrayClone() {
    int[] array = trackedIntArray("abc");
    assertThatTrackerOf(array.clone()).matches(snapshot().track(array));
  }

  // regression test for NPE in analysis when byte[] type is null
  @SuppressWarnings("ConstantValue")
  @Test public void byteArrayNull() {
    byte[] a = trackedByteArray("a");

    byte[] bytes = null;
    if (bytes == null) {
      bytes = new byte[1];
    }
    bytes[0] = a[0];

    // would be nice if this was tracked. for now, we're happy with it not blowing up
    // snapshotBuilder().track(a, 0, 1).assertTrackerOf(bytes);
    assertThat(getTracker(context(), bytes)).isNull();
  }

  @Test public void intArrayLoadAndStore() {
    int[] abc = trackedIntArray("abc");

    int[] array = new int[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    assertThatTrackerOf(array).matches(
        snapshot().track(1, abc, 1).track(1, abc, 0).track(1, abc, 2));
  }

  /**
   * Regression test that tests that when we get a value out of an array, we find out where it came
   * from at that point; and not at the moment use that value because by then the value in the array
   * might have already changed.
   */
  @Test public void arrayLoadMutatedBeforeUse() {
    FlowTester ft2 = new FlowTester();

    char[] array1 = new char[1];
    array1[0] = ft.createSourceChar('a');

    // testing that we track where gotA and gotB come from based on the time they were read; not
    // the time they were used
    char gotA = array1[0];
    array1[0] = ft2.createSourceChar('b');
    char gotB = array1[0];

    char[] target = new char[2];
    target[0] = gotA;
    target[1] = gotB;

    TrackerSnapshot.assertThatTrackerOf(target).matches(TrackerSnapshot.snapshot()
        .part(ft.point())
        .part(ft2.point()));
  }
}
