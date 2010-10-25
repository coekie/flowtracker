package be.coekaerts.wouter.flowtracker.test;

import java.util.Map.Entry;

import junit.framework.Assert;
import be.coekaerts.wouter.flowtracker.tracker.TrackPart;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

/**
 * Helper methods for testing Trackers
 */
public class TrackTestHelper {
	
	public static String trackCopy(String str) {
		str = new String(str);
		TrackerRepository.createTracker(str);
		return str;
	}

	public static TrackPart part(String str) {
		return part(str, 0, str.length());
	}
	
	public static TrackPart part(Object obj, int index, int length) {
		Tracker tracker = TrackerRepository.getTracker(obj);
		Assert.assertNotNull(tracker);
		return new TrackPart(tracker, index, length);
	}
	
	/**
	 * Asserts that the tracker consist of the given parts, starting at index 0 without holes.
	 */
	public static void assertPartsCompleteEqual(Object target, TrackPart... expectedParts) {
		int partNr = 0;
		Assert.assertNotNull(target);
		Tracker tracker = TrackerRepository.getTracker(target);
		Assert.assertNotNull(tracker);
		Assert.assertEquals(tracker.getEntryCount(), expectedParts.length);
		int index = 0;
		for (TrackPart expectedPart : expectedParts) {
			assertEntryEquals("Part " + partNr, index, expectedPart.getLength(), expectedPart.getTracker(),
					expectedPart.getIndex(), tracker.getEntryAt(index));
			index += expectedPart.getLength();
			partNr++;
		}
	}

//	public static void assertEntryEquals(int expectedEntryIndex, int expectedLength,
//			Tracker expectedTracker, int expectedPartIndex, Entry<Integer, TrackPart> entry) {
//		assertEntryEquals("", expectedEntryIndex, expectedLength, expectedTracker, expectedPa)
//	}
	
	public static void assertEntryEquals(String message, int expectedEntryIndex, int expectedLength,
				Tracker expectedTracker, int expectedPartIndex, Entry<Integer, TrackPart> entry) {
		String prefix = message + ": ";
		Assert.assertNotNull(prefix + "entry", entry);
		Assert.assertEquals(prefix + "entry index", (Integer)expectedEntryIndex, entry.getKey());
		
		TrackPart part = entry.getValue();
		Assert.assertNotNull(prefix + "part", part);
		Assert.assertEquals(prefix + "length", expectedLength, part.getLength());
		Assert.assertSame(prefix + "tracker", expectedTracker, part.getTracker());
		Assert.assertEquals(prefix + "part index", expectedPartIndex, part.getIndex());
	}
}
