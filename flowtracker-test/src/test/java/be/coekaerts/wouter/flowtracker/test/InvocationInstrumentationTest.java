package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;

import org.junit.Test;

public class InvocationInstrumentationTest {
  @Test
  public void test() {
    char[] array = TrackTestHelper.trackedCharArray("abc");
    ArrayWrapper wrapper = new ArrayWrapper(array);
    FlowTester.assertTrackedValue((byte) wrapper.read(2), getTracker(array), 2);
  }

  static class ArrayWrapper {
    private final char[] array;

    ArrayWrapper(char[] array) {
      this.array = array;
    }

    int read(int pos) {
      return array[pos];
    }
  }
}
