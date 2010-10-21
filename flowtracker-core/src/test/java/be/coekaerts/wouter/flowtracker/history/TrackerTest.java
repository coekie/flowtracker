package be.coekaerts.wouter.flowtracker.history;

import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TrackerTest {
	private Object source, source2;
	private Tracker sourceTracker, sourceTracker2;
	private Object target;
	
	@Before
	public void setupSource() {
		source = new Object();
		sourceTracker = TrackerRepository.createTracker(source);
		source2 = new Object();
		sourceTracker2 = TrackerRepository.createTracker(source2);
		target = new Object();
	}
	
	@Test
	public void testSetSingleSource() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertNotNull(targetTracker);
		Assert.assertEquals(1, targetTracker.getEntryCount());
		
		
		// check the right entry is at position 5,6 and 7
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(6));
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(7));
		
		// check that the ones before and after are null
		Assert.assertNull(targetTracker.getEntryAt(4));
		Assert.assertNull(targetTracker.getEntryAt(8));
	}
	
	/**
	 * Set one source, then a second one after it, leaving a gap in between
	 */
	@Test
	public void setSecondSourceAfterGap() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7
		Tracker.setSource(target, 9, 2, source, 14); // setting 9,10, leaving a gap at 8
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(2, targetTracker.getEntryCount());
		
		// entry at 5 is still there
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		
		// second one at 9 and 10		
		assertEntryEquals(9, 2, sourceTracker, 14, targetTracker.getEntryAt(9));
		assertEntryEquals(9, 2, sourceTracker, 14, targetTracker.getEntryAt(10));
		
		// but not in between and after
		Assert.assertNull(targetTracker.getEntryAt(8));
		Assert.assertNull(targetTracker.getEntryAt(11));
		
	}
	
	/**
	 * Set one source, then a second one before it, leaving a gap in between
	 */
	@Test
	public void setSecondSourceBeforeGap() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7
		Tracker.setSource(target, 2, 2, source, 8); // setting 2,3, leaving a gap at 4
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(2, targetTracker.getEntryCount());
		
		// entry at 5 is still there
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		
		// second one at 2 and 3		
		assertEntryEquals(2, 2, sourceTracker, 8, targetTracker.getEntryAt(2));
		assertEntryEquals(2, 2, sourceTracker, 8, targetTracker.getEntryAt(3));
		
		// but not in between and before
		Assert.assertNull(targetTracker.getEntryAt(1));
		Assert.assertNull(targetTracker.getEntryAt(4));
		
	}
	
	/**
	 * Merge an entry if it comes after an existing one that matches
	 */
	@Test
	public void testMergeAfter() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source, 14); // setting 8,9 to 14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(9));
	}
	
	/**
	 * Don't merge an entry if it comes right after one, but the indices don't match
	 */
	@Test
	public void testDontMergeAfterSkipped() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source, 15); // setting 8,9 to 15,16 (skipping 14) 
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(2, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(8, 2, sourceTracker, 15, targetTracker.getEntryAt(9));
	}
	
	/**
	 * Merge an entry if it comes before an existing one that matches
	 */
	@Test
	public void testMergeBefore() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 3, 2, source, 9); // setting 3,4 to 9,10
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(3, 5, sourceTracker, 11, targetTracker.getEntryAt(3));
		assertEntryEquals(3, 5, sourceTracker, 11, targetTracker.getEntryAt(7));
	}
	
	/**
	 * Don't merge an entry if it comes right before one, but the indices don't match
	 */
	@Test
	public void testDontMergeBeforeSkipped() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 3, 2, source, 8); // setting 3,4 to 8, 9 
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(2, targetTracker.getEntryCount());
		
		assertEntryEquals(3, 2, sourceTracker, 8, targetTracker.getEntryAt(3));
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
	}
	
	/**
	 * Don't merge entries with a different source
	 */
	@Test
	public void testDontMergeDifferentSource() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source2, 14); // setting 8,9 to 14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(2, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(8, 2, sourceTracker2, 14, targetTracker.getEntryAt(8));
	}
	
	/**
	 * Insert the missing part in between two entries with a hole, expect the three to merge
	 */
	@Test
	@Ignore("not implemented")
	public void testMergeMiddle() {
		Tracker.setSource(target, 10, 2, source, 16); // setting 10,11 to 16,17
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source, 14); // setting 8,9 to 14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 7, sourceTracker, 11, targetTracker.getEntryAt(5));
	}
	
	@Test
	@Ignore("not implemented")
	public void testOverlapMergeAfter() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 6, 4, source, 11); // setting 6,7,8,9 to 12,13,14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		Assert.assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(5));
	}
	
	// TODO testOverlapMergeBefore
	// TODO testOverlapOutsideMerge
	// TODO testOverlapInsideMerge
	
	// TODO testCutBefore
	// TODO testCutAfter
	// TODO testCutInside
	// TODO testMultiCut
	
	private void assertEntryEquals(int expectedEntryIndex, int expectedLength,
			Tracker expectedTracker, int expectedPartIndex, Entry<Integer, TrackPart> entry) {
		Assert.assertNotNull(entry);
		Assert.assertEquals((Integer)expectedEntryIndex, entry.getKey());
		
		TrackPart part = entry.getValue();
		Assert.assertNotNull(part);
		Assert.assertEquals(expectedLength, part.getLength());
		Assert.assertSame(expectedTracker, part.getTracker());
		Assert.assertEquals(expectedPartIndex, part.getIndex());
	}
}
