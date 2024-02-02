package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Helper methods for testing Trackers
 */
public class TrackTestHelper {

  /**
   * Returns a copy of the given String, which will be tracked (by a {@link FixedOriginTracker}).
   * <p>
   * We create a copy to avoid interference from other usage of the same (interned) String.
   */
  public static String trackCopy(String str) {
    str = new String(str.toCharArray());
    StringHook.createFixedOriginTracker(str);
    return str;
  }

  /** Create a tracked char array, with the given length (by a {@link FixedOriginTracker}) */
  public static char[] trackedCharArrayWithLength(int length) {
    char[] result = new char[length];
    TrackerRepository.createFixedOriginTracker(result, length);
    return result;
  }

  /** Create a tracked char array (by a {@link FixedOriginTracker}) */
  public static char[] trackedCharArray(String str) {
    char[] result = str.toCharArray();
    TrackerRepository.createFixedOriginTracker(result, result.length);
    return result;
  }

  /** Create a char array without tracking (by a {@link FixedOriginTracker}) */
  public static char[] untrackedCharArray(String str) {
    char[] result = str.toCharArray();
    if (TrackerRepository.getTracker(result) != null) {
      throw new IllegalStateException("Did not expect result to be tracked");
    }
    return result;
  }

  /** Create a tracked byte array (by a {@link FixedOriginTracker}) */
  public static byte[] trackedByteArray(String str) {
    byte[] result = str.getBytes();
    TrackerRepository.createFixedOriginTracker(result, result.length);
    return result;
  }

  /** Create a byte array without tracking */
  public static byte[] untrackedByteArray(String str) {
    byte[] result = str.getBytes();
    if (TrackerRepository.getTracker(result) != null) {
      throw new IllegalStateException("Did not expect result to be tracked");
    }
    return result;
  }

  /** Fluent API to assert stuff about trackers */
  public static TrackerAssertions assertThatTracker(Object sut) {
    return new TrackerAssertions(tracker(sut));
  }

  private static List<String> nodePath(Node node) {
    List<String> result = new ArrayList<>();
    while (node != null && node.parent != null) {
      result.add(node.name);
      node = node.parent;
    }
    Collections.reverse(result);
    return result;
  }

  private static Tracker tracker(Object o) {
    return o instanceof Tracker ? (Tracker) o : TrackerRepository.getTracker(o);
  }

  /** @see #assertThatTracker(Object) */
  public static class TrackerAssertions {
    private final Tracker tracker;

    private TrackerAssertions(Tracker tracker) {
      assertNotNull(tracker);
      this.tracker = tracker;
    }

    public TrackerAssertions hasDescriptor(String expectedDescriptor) {
      assertEquals(expectedDescriptor, tracker.getDescriptor());
      return this;
    }

    public TrackerAssertions hasDescriptorMatching(Predicate<String> predicate) {
      assertTrue(predicate.test(tracker.getDescriptor()));
      return this;
    }

    public TrackerAssertions hasNode(String... expectedNodePath) {
      assertEquals(Arrays.asList(expectedNodePath), nodePath(tracker.getNode()));
      return this;
    }

    public TrackerAssertions hasNodeMatching(Predicate<List<String>> predicate) {
      assertTrue(predicate.test(nodePath(tracker.getNode())));
      return this;
    }
  }
}
