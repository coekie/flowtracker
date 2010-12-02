package be.coekaerts.wouter.flowtracker.test;

import java.util.Map.Entry;

import junit.framework.Assert;
import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerDepth;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

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
		Assert.assertNotNull(tracker);
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
		Assert.assertNotNull(target);
		assertTrackerPartsCompleteEqual(TrackerRepository.getTracker(target), expectedParts);
	}
	
	/**
	 * Asserts that the tracker consist of the given parts, starting at index 0 without holes.
	 */
	public static void assertTrackerPartsCompleteEqual(Tracker tracker, PartTracker... expectedParts) {
		Assert.assertNotNull(tracker);
		Assert.assertEquals(expectedParts.length, tracker.getEntryCount());
		
		int partNr = 0;
		int index = 0;
		for (PartTracker expectedPart : expectedParts) {
			assertEntryEquals("Part " + partNr, index, expectedPart.getLength(), expectedPart.getTracker(),
					expectedPart.getIndex(), tracker.getEntryAt(index));
			index += expectedPart.getLength();
			partNr++;
		}
	}

//	public static void assertEntryEquals(int expectedEntryIndex, int expectedLength,
//			Tracker expectedTracker, int expectedPartIndex, Entry<Integer, PartTracker> entry) {
//		assertEntryEquals("", expectedEntryIndex, expectedLength, expectedTracker, expectedPa)
//	}
	
	public static void assertEntryEquals(String message, int expectedEntryIndex, int expectedLength,
				Tracker expectedTracker, int expectedPartIndex, Entry<Integer, PartTracker> entry) {
		String prefix = message + ": ";
		Assert.assertNotNull(prefix + "entry", entry);
		Assert.assertEquals(prefix + "entry index", (Integer)expectedEntryIndex, entry.getKey());
		
		PartTracker part = entry.getValue();
		Assert.assertNotNull(prefix + "part", part);
		Assert.assertEquals(prefix + "length", expectedLength, part.getLength());
		Assert.assertSame(prefix + "tracker", expectedTracker, part.getTracker());
		Assert.assertEquals(prefix + "part index", expectedPartIndex, part.getIndex());
	}
}
