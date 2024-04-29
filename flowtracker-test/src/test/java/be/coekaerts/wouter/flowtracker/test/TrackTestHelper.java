package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
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

  /** Create a tracked int array (by a {@link FixedOriginTracker}) */
  public static int[] trackedIntArray(String str) {
    int[] result = str.codePoints().toArray();
    TrackerRepository.createFixedOriginTracker(result, result.length);
    return result;
  }

  /** Create a String without tracking */
  public static String untrackedString(String str) {
    String result = new String(str.getBytes());
    StringHook.removeTracker(result);
    return result;
  }

  /** Fluent API to assert stuff about trackers */
  public static NodeAssertions assertThatTrackerNode(Object sut) {
    return new NodeAssertions(tracker(sut));
  }

  private static Tracker tracker(Object o) {
    return o instanceof Tracker ? (Tracker) o : TrackerRepository.getTracker(o);
  }

  /** @see #assertThatTrackerNode(Object) */
  public static class NodeAssertions {
    private final Node node;

    private NodeAssertions(Tracker tracker) {
      assertThat(tracker).isNotNull();
      this.node = tracker.getNode();
    }

    public NodeAssertions hasPath(String... expectedNodePath) {
      assertThat(node.path()).containsExactlyElementsIn(expectedNodePath).inOrder();
      return this;
    }

    public NodeAssertions hasPathStartingWith(String... expectedNodePath) {
      assertThat(node.path().subList(0, expectedNodePath.length))
          .containsExactlyElementsIn(expectedNodePath).inOrder();
      return this;
    }

    public NodeAssertions hasPathEndingWith(String... expectedNodePath) {
      List<String> path = node.path();
      assertThat(path.subList(path.size() - expectedNodePath.length, path.size()))
          .containsExactlyElementsIn(expectedNodePath).inOrder();
      return this;
    }

    public NodeAssertions hasPathMatching(Predicate<List<String>> predicate) {
      List<String> nodePath = node.path();
      if (!predicate.test(nodePath)) {
        throw new AssertionError(nodePath.toString());
      }
      return this;
    }
  }
}
