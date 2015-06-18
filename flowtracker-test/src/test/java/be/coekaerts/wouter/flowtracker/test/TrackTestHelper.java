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

  /** Adds a tracker on the given object and returns it. */
  public static char[] track(char[] chars) {
    TrackerRepository.createFixedOriginTracker(chars, chars.length);
    return chars;
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
