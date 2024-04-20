package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
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
    TrackerRepository.removeTracker(result);
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
    TrackerRepository.removeTracker(result);
    return result;
  }

  /** Create a String without tracking */
  public static String untrackedString(String str) {
    String result = new String(str.getBytes());
    StringHook.removeTracker(result);
    return result;
  }

  /** Fluent API to assert stuff about trackers */
  public static TrackerAssertions assertThatTracker(Object sut) {
    return new TrackerAssertions(tracker(sut));
  }

  private static Tracker tracker(Object o) {
    return o instanceof Tracker ? (Tracker) o : TrackerRepository.getTracker(o);
  }

  /** @see #assertThatTracker(Object) */
  public static class TrackerAssertions {
    private final Tracker tracker;

    private TrackerAssertions(Tracker tracker) {
      assertThat(tracker).isNotNull();
      this.tracker = tracker;
    }

    public TrackerAssertions hasNode(String... expectedNodePath) {
      assertThat(tracker.getNode().path()).containsExactlyElementsIn(expectedNodePath).inOrder();
      return this;
    }

    public TrackerAssertions hasNodeStartingWith(String... expectedNodePath) {
      assertThat(tracker.getNode().path().subList(0, expectedNodePath.length))
          .containsExactlyElementsIn(expectedNodePath).inOrder();
      return this;
    }

    public TrackerAssertions hasNodeEndingWith(String... expectedNodePath) {
      List<String> path = tracker.getNode().path();
      assertThat(path.subList(path.size() - expectedNodePath.length, path.size()))
          .containsExactlyElementsIn(expectedNodePath).inOrder();
      return this;
    }

    public TrackerAssertions hasNodeMatching(Predicate<List<String>> predicate) {
      List<String> nodePath = tracker.getNode().path();
      if (!predicate.test(nodePath)) {
        throw new AssertionError(nodePath.toString());
      }
      return this;
    }
  }
}
