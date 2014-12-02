package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerDepth;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.util.Map.Entry;

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
	
	/**
	 * Adds a tracker on the given object and returns it.
	 */
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
	
	public static void assertStringOriginPartsCompleteEqual(String target, PartTracker... expectedParts) {
		assertTrackerPartsCompleteEqual(
				DefaultTracker.copyOf(StringHook.getStringTrack(target), TrackerDepth.ORIGIN),
				expectedParts);
	}
	
	/**
	 * Asserts that the tracker of the given target consist of the given parts, starting at index 0 without holes.
	 */
	public static void assertPartsCompleteEqual(Object target, PartTracker... expectedParts) {
		assertNotNull(target);
		assertTrackerPartsCompleteEqual(TrackerRepository.getTracker(target), expectedParts);
	}
	
	/**
	 * Asserts that the tracker consist of the given parts, starting at index 0 without holes.
	 */
	public static void assertTrackerPartsCompleteEqual(Tracker tracker, PartTracker... expectedParts) {
		assertNotNull(tracker);
		assertEquals(expectedParts.length, tracker.getEntryCount());
		
		int partNr = 0;
		int index = 0;
		for (PartTracker expectedPart : expectedParts) {
			assertEntryEquals("Part " + partNr, index, expectedPart.getLength(), expectedPart.getTracker(),
					expectedPart.getIndex(), tracker.getEntryAt(index));
			index += expectedPart.getLength();
			partNr++;
		}
	}

	public static void assertEntryEquals(String message, int expectedEntryIndex, int expectedLength,
				Tracker expectedTracker, int expectedPartIndex, Entry<Integer, PartTracker> entry) {
		String prefix = message + ": ";
		assertNotNull(prefix + "entry", entry);
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
  static void assertInterestAndDescriptor(Object sut, String expectedDescriptor, Object expectedDescriptorObj) {
    ContentTracker tracker = TrackerRepository.getContentTracker(sut);
    assertNotNull(tracker);
    assertTrue(InterestRepository.getContentTrackers().contains(tracker));
    assertEquals(expectedDescriptor, tracker.getDescriptor());
    assertSame(TrackerRepository.getTracker(expectedDescriptorObj), tracker.getDescriptorTracker());
  }
}
