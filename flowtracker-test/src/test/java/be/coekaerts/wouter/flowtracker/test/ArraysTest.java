package be.coekaerts.wouter.flowtracker.test;

import java.util.Arrays;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;

/** Test for {@link Arrays}. */
public class ArraysTest {
  @Test public void copyOf() {
    char[] abcdef = track("abcdef".toCharArray());
    char[] abcd = Arrays.copyOf(abcdef, 4);

    snapshotBuilder().track(abcdef, 0, 4)
        .assertEquals(getTracker(abcd));
  }

  @Test public void copyOfRange() {
    char[] abcdef = track("abcdef".toCharArray());
    char[] bcde = Arrays.copyOfRange(abcdef, 1, 5);
    snapshotBuilder().track(abcdef, 1, 4)
        .assertEquals(getTracker(bcde));
  }
}
