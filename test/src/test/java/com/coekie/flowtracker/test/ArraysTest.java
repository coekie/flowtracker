package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.util.Arrays;
import org.junit.Test;

/**
 * Test for {@link Arrays}.
 *
 * @see ArrayTest
 */
public class ArraysTest {
  @Test public void copyOf() {
    char[] abcdef = TrackTestHelper.trackedCharArray("abcdef");
    char[] abcd = Arrays.copyOf(abcdef, 4);

    TrackerSnapshot.assertThatTracker(TrackerRepository.getTracker(context(), abcd)).matches(
        TrackerSnapshot.snapshot().track(4, abcdef, 0));
  }

  @Test public void copyOfRange() {
    char[] abcdef = TrackTestHelper.trackedCharArray("abcdef");
    char[] bcde = Arrays.copyOfRange(abcdef, 1, 5);
    TrackerSnapshot.assertThatTracker(TrackerRepository.getTracker(context(), bcde)).matches(
        TrackerSnapshot.snapshot().track(4, abcdef, 1));
  }
}
