package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Helper methods for testing Trackers
 */
public class TrackTestHelper {

  /**
   * Returns a copy of the given String, which will be tracked.
   *
   * We create a copy to avoid interference from other usage of the same (interned) String.
   */
  public static String trackCopy(String str) {
    str = new String(str.toCharArray());
    StringHook.createFixedOriginTracker(str);
    return str;
  }

  /** Create a tracked char array, with the given length */
  public static char[] trackedCharArrayWithLength(int length) {
    char[] result = new char[length];
    TrackerRepository.createFixedOriginTracker(result, length);
    return result;
  }

  /** Create a tracked char array */
  public static char[] trackedCharArray(String str) {
    char[] result = str.toCharArray();
    TrackerRepository.createFixedOriginTracker(result, result.length);
    return result;
  }

  /** Create a char array without tracking */
  public static char[] untrackedCharArray(String str) {
    char[] result = str.toCharArray();
    if (TrackerRepository.getTracker(result) != null) {
      throw new IllegalStateException("Did not expect result to be tracked");
    }
    return result;
  }

  /** Create a tracked byte array */
  public static byte[] trackedByteArray(String str) {
    byte[] result = str.getBytes();
    TrackerRepository.createFixedOriginTracker(result, result.length);
    return result;
  }

  /**
   * Assert that the given object is in the InterestRepository and has a descriptor with the given
   * message and object
   */
  static void assertInterestAndDescriptor(Object sut, String expectedDescriptor,
      Object expectedDescriptorObj) {
    assertDescriptor(sut, expectedDescriptor, expectedDescriptorObj);
    assertTrue(InterestRepository.getTrackers().contains(TrackerRepository.getTracker(sut)));
  }

  static void assertDescriptor(Object sut, String expectedDescriptor,
      Object expectedDescriptorObj) {
    Tracker tracker = TrackerRepository.getTracker(sut);
    assertNotNull(tracker);
    assertEquals(expectedDescriptor, tracker.getDescriptor());
    assertSame(TrackerRepository.getTracker(expectedDescriptorObj), tracker.getDescriptorTracker());
  }
}
