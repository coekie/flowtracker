package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertStringOriginPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertTrackerPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.strPart;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import junit.framework.Assert;

import org.junit.Test;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

public class StringTest {
	@Test
	public void unkownTest() {
		String a = "unkownTest";
		Assert.assertNull(TrackerRepository.getTracker(a));
		Assert.assertNull(StringHook.getStringTrack(a));
	}
	
	@Test
	public void concatTest() {
		String a = trackCopy("concatTest1");
		String b = trackCopy("concatTest2");
		String ab = a.concat(b);
		Assert.assertEquals("concatTest1concatTest2", ab);
		
		assertStringOriginPartsCompleteEqual(ab, strPart(a), strPart(b));
	}
	
	@Test
	public void substringBeginTest()  {
		String foobar = trackCopy("foobar");
		String foo = foobar.substring(0, 3);
		Assert.assertEquals("foo", foo);
		
		assertStringOriginPartsCompleteEqual(foo, strPart(foobar, 0, 3));
	}
	
	@Test
	public void substringEndTest()  {
		String foobar = trackCopy("foobar");
		String bar = foobar.substring(3);
		Assert.assertEquals("bar", bar);
		
		assertStringOriginPartsCompleteEqual(bar, strPart(foobar, 3, 3));
	}
	
	@Test
	public void stringTrackTest() {
		char[] chars = track(new char[]{'a', 'b', 'c', 'd'});
		String str = new String(chars, 1, 2); // create String "bc"
		Assert.assertEquals("bc", str);
		
		// stringTrack points to the String.value char[]
		PartTracker stringTrack = StringHook.getStringTrack(str);
		Assert.assertNotNull(stringTrack);
		Assert.assertEquals(0, stringTrack.getIndex());
		Assert.assertEquals(2, stringTrack.getLength());
		
		// String.value is copy of part of our chars
		Tracker stringValueTracker = stringTrack.getTracker();
		Assert.assertNotNull(stringValueTracker);
		assertTrackerPartsCompleteEqual(stringValueTracker, part(chars, 1, 2));
	}
	
	/**
	 * Test that we didn't break the normal functioning of contentEquals.
	 */
	@Test
	public void contentEqualsTest() {
		String str = "abcd";
		StringBuilder sb = new StringBuilder();
		sb.append("ab").append("cd");
		Assert.assertTrue(str.contentEquals(sb));
		Assert.assertFalse(str.contentEquals("foo"));
	}
}
