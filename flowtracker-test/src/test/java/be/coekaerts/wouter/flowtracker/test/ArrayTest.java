package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedIntArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

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
    FlowTester.assertTrackedValue(array[2], '\0', getTracker(array), 2);
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
    assertThat(getTracker(bytes)).isNull();
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
}
