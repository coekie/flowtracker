package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;

import java.util.Arrays;
import org.junit.Test;

/** Test for {@link Arrays}. */
public class ArraysTest {
  @Test public void copyOf() {
    char[] abcdef = TrackTestHelper.trackedCharArray("abcdef");
    char[] abcd = Arrays.copyOf(abcdef, 4);

    assertThatTracker(getTracker(abcd)).matches(snapshot().track(abcdef, 0, 4));
  }

  @Test public void copyOfRange() {
    char[] abcdef = TrackTestHelper.trackedCharArray("abcdef");
    char[] bcde = Arrays.copyOfRange(abcdef, 1, 5);
    assertThatTracker(getTracker(bcde)).matches(snapshot().track(abcdef, 1, 4));
  }
}
