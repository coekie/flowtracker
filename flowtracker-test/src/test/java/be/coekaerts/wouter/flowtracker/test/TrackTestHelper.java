package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

  public static PartTracker strPart(String str) {
    return StringHook.getStringTrack(str);
  }

  public static PartTracker strPart(String str, int index, int length) {
    PartTracker fullPart = StringHook.getStringTrack(str);
    return new PartTracker(fullPart.getTracker(), fullPart.getIndex() + index, length);
  }

  public static PartTracker part(Object obj, int index, int length) {
    Tracker tracker = TrackerRepository.getTracker(obj);
    assertNotNull(tracker);
    return new PartTracker(tracker, index, length);
  }

  public static PartTracker gap(int length) {
    return new PartTracker(null, 0, length);
  }

  /**
   * Asserts that the tracker of the given target consist of the given parts, starting at index 0
   * without holes.
   */
  public static void assertPartsCompleteEqual(Object target, PartTracker... expectedParts) {
    assertNotNull(target);
    assertTrackerPartsCompleteEqual(TrackerRepository.getTracker(target), expectedParts);
  }

  /** Asserts that the tracker consist of the given parts, starting at index 0 without holes. */
  public static void assertTrackerPartsCompleteEqual(Tracker tracker,
      PartTracker... expectedParts) {
    assertNotNull(tracker);

    int partNr = 0;
    int index = 0;
    for (PartTracker expectedPart : expectedParts) {
      if (expectedPart.getTracker() == null) { // a gap
        assertNull(tracker.getEntryAt(index));
      } else {
        assertEntryEquals("Part " + partNr, index, expectedPart.getLength(),
            expectedPart.getTracker(),
            expectedPart.getIndex(), tracker.getEntryAt(index));
        partNr++;
      }
      index += expectedPart.getLength();
    }

    assertEquals(partNr, tracker.getEntryCount());
  }

  private static void assertEntryEquals(String message, int expectedEntryIndex, int expectedLength,
      Tracker expectedTracker, int expectedPartIndex, Entry<Integer, PartTracker> entry) {
    String prefix = message + ": ";
    assertNotNull(prefix + "entry exists", entry);
    assertEquals(prefix + "entry index", (Integer) expectedEntryIndex, entry.getKey());

    PartTracker part = entry.getValue();
    assertNotNull(prefix + "part", part);
    assertEquals(prefix + "length", expectedLength, part.getLength());
    assertSame(prefix + "tracker", expectedTracker, part.getTracker());
    assertEquals(prefix + "part index", expectedPartIndex, part.getIndex());
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
