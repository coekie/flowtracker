package be.coekaerts.wouter.flowtracker.history;

import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;

public class TrackerTest {
	private Object source, source2;
	private Tracker sourceTracker, sourceTracker2;
	private Object target;
	private Object middleman;
	
	@Before
	public void setupSource() {
		source = new Object();
		sourceTracker = TrackerRepository.createFixedOriginTracker(source, 100);
		source2 = new Object();
		sourceTracker2 = TrackerRepository.createFixedOriginTracker(source2, 100);
		target = new Object();
		middleman = new Object();
	}
	
	@Test
	public void testSetSingleSource() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertNotNull(targetTracker);
		assertEquals(1, targetTracker.getEntryCount());
		
		
		// check the right entry is at position 5,6 and 7
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(6));
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(7));
		
		// check that the ones before and after are null
		assertNull(targetTracker.getEntryAt(4));
		assertNull(targetTracker.getEntryAt(8));
	}
	
	/**
	 * Set one source, then a second one after it, leaving a gap in between
	 */
	@Test
	public void setSecondSourceAfterGap() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7
		Tracker.setSource(target, 9, 2, source, 14); // setting 9,10, leaving a gap at 8
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(2, targetTracker.getEntryCount());
		
		// entry at 5 is still there
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		
		// second one at 9 and 10		
		assertEntryEquals(9, 2, sourceTracker, 14, targetTracker.getEntryAt(9));
		assertEntryEquals(9, 2, sourceTracker, 14, targetTracker.getEntryAt(10));
		
		// but not in between and after
		assertNull(targetTracker.getEntryAt(8));
		assertNull(targetTracker.getEntryAt(11));
		
	}
	
	/**
	 * Set one source, then a second one before it, leaving a gap in between
	 */
	@Test
	public void setSecondSourceBeforeGap() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7
		Tracker.setSource(target, 2, 2, source, 8); // setting 2,3, leaving a gap at 4
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(2, targetTracker.getEntryCount());
		
		// entry at 5 is still there
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		
		// second one at 2 and 3		
		assertEntryEquals(2, 2, sourceTracker, 8, targetTracker.getEntryAt(2));
		assertEntryEquals(2, 2, sourceTracker, 8, targetTracker.getEntryAt(3));
		
		// but not in between and before
		assertNull(targetTracker.getEntryAt(1));
		assertNull(targetTracker.getEntryAt(4));
		
	}
	
	/**
	 * Merge an entry if it comes after an existing one that matches
	 */
	@Test
	public void testTouchMergeWithPrevious() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source, 14); // setting 8,9 to 14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(9));
	}
	
	/**
	 * Don't merge an entry if it comes right after one, but the indices don't match
	 */
	@Test
	public void testTouchDontMergeWithPreviousWhenSkipped() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source, 15); // setting 8,9 to 15,16 (skipping 14) 
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(2, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(8, 2, sourceTracker, 15, targetTracker.getEntryAt(9));
	}
	
	/**
	 * Merge an entry if it comes before an existing one that matches
	 */
	@Test
	public void testTouchMergeWithNext() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 3, 2, source, 9); // setting 3,4 to 9,10
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(3, 5, sourceTracker, 9, targetTracker.getEntryAt(3));
		assertEntryEquals(3, 5, sourceTracker, 9, targetTracker.getEntryAt(7));
	}
	
	/**
	 * Don't merge an entry if it comes right before one, but the indices don't match
	 */
	@Test
	public void testTouchDontMergeWithNextWhenSkipped() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 3, 2, source, 8); // setting 3,4 to 8, 9 
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(2, targetTracker.getEntryCount());
		
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
		assertEquals(2, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
		assertEntryEquals(8, 2, sourceTracker2, 14, targetTracker.getEntryAt(8));
	}
	
	/**
	 * Insert the missing part in between two entries with a hole, expect the three to merge
	 */
	@Test
	public void testTouchMergeWithPreviousAndNext() {
		Tracker.setSource(target, 10, 2, source, 16); // setting 10,11 to 16,17
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 8, 2, source, 14); // setting 8,9 to 14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 7, sourceTracker, 11, targetTracker.getEntryAt(5));
	}
	
	@Test
	public void testOverlapMergeWithPrevious() {
		Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
		Tracker.setSource(target, 6, 4, source, 12); // setting 6,7,8,9 to 12,13,14,15
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(1, targetTracker.getEntryCount());
		
		assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(5));
	}

  @Test
  public void testOverlapMergeWithNext() {
    Tracker.setSource(target, 6, 4, source, 12); // setting 6,7,8,9 to 12,13,14,15
    Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(1, targetTracker.getEntryCount());

    assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(5));
  }

  @Test
  public void testOverlapOverwritePrevious() {
    Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
    Tracker.setSource(target, 6, 4, source, 100); // setting 6,7,8,9 to 100,101,102,103

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(2, targetTracker.getEntryCount());

    assertEntryEquals(5, 1, sourceTracker, 11, targetTracker.getEntryAt(5));
    assertEntryEquals(6, 4, sourceTracker, 100, targetTracker.getEntryAt(6));
  }

  @Test
  public void testOverlapOverwriteNext() {
    Tracker.setSource(target, 6, 4, source, 100); // setting 6,7,8,9 to 100,101,102,103
    Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(2, targetTracker.getEntryCount());

    assertEntryEquals(5, 3, sourceTracker, 11, targetTracker.getEntryAt(5));
    assertEntryEquals(8, 2, sourceTracker, 102, targetTracker.getEntryAt(8));
  }

  @Test
  public void testOverlapOverwriteCompletely() {
    Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
    Tracker.setSource(target, 4, 5, source, 100); // setting 4,5,6,7,8 to 100,...

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(1, targetTracker.getEntryCount());

    assertEntryEquals(4, 5, sourceTracker, 100, targetTracker.getEntryAt(4));
  }

  @Test
  public void testOverwriteExactSame() {
    Tracker.setSource(target, 5, 3, source, 11); // setting 5,6,7 to 11,12,13
    Tracker.setSource(target, 5, 3, source, 100); // setting 5,6,7 to 100,101,102

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(1, targetTracker.getEntryCount());

    assertEntryEquals(5, 3, sourceTracker, 100, targetTracker.getEntryAt(5));
  }

  @Test public void testOverlapOverwriteMultiple() {
    Tracker.setSource(target, 5, 3, source, 10);
    Tracker.setSource(target, 8, 3, source, 20);
    Tracker.setSource(target, 11, 3, source, 30);
    Tracker.setSource(target, 14, 3, source, 40);
    Tracker.setSource(target, 6, 10, source, 100);

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(3, targetTracker.getEntryCount());

    assertEntryEquals(5, 1, sourceTracker, 10, targetTracker.getEntryAt(5));
    assertEntryEquals(6, 10, sourceTracker, 100, targetTracker.getEntryAt(6));
    assertEntryEquals(16, 1, sourceTracker, 42, targetTracker.getEntryAt(16));
  }

  @Test
  public void testOverwriteMiddle() {
    Tracker.setSource(target, 5, 5, source, 11); // setting 5,6,7,8,9 to 11,12,13,14,15
    Tracker.setSource(target, 6, 2, source, 100); // setting 6,7 to 100,101

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(3, targetTracker.getEntryCount());

    assertEntryEquals(5, 1, sourceTracker, 11, targetTracker.getEntryAt(5));
    assertEntryEquals(6, 2, sourceTracker, 100, targetTracker.getEntryAt(6));
    assertEntryEquals(8, 2, sourceTracker, 14, targetTracker.getEntryAt(8));
  }

  @Test
  public void testOverwriteMiddleWithSame() {
    Tracker.setSource(target, 5, 5, source, 11); // setting 5,6,7,8,9 to 11,12,13,14,15
    Tracker.setSource(target, 6, 2, source, 12); // setting 6,7 to 12,13 again

    Tracker targetTracker = TrackerRepository.getTracker(target);
    assertEquals(1, targetTracker.getEntryCount());

    assertEntryEquals(5, 5, sourceTracker, 11, targetTracker.getEntryAt(5));
  }

	/** Use the source of the source if the direct source is mutable */
	@Test
	public void testMutableMiddleman() {
		Tracker.setSource(middleman, 0, 10, source, 0);
		Tracker.setSource(target, 0, 10, middleman, 0);
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEntryEquals(0, 10, sourceTracker, 0, targetTracker.getEntryAt(0));
		
		// Even after changing middleman to another source,
		Tracker.setSource(middleman, 0, 10, source2, 0);
		// target still knows it came from first source.
		assertEntryEquals(0, 10, sourceTracker, 0, targetTracker.getEntryAt(0));
		
		assertEquals(1, targetTracker.getEntryCount());
	}
	
	/**
	 * Get the source of the source composed of two parts,
	 * but only partly: dropping the begin and ending 
	 */
	@Test
	public void testTransitiveCombinePartParts() {
		// set middleman to 100,101,102,103,104,200,201,202,203
		Tracker.setSource(middleman, 0, 5, source, 100);
		Tracker.setSource(middleman, 5, 4, source2, 200);
		// set target to 101,102,103,104,200,201,202
		Tracker.setSource(target, 0, 7, middleman, 1);
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		
		assertEquals(2, targetTracker.getEntryCount());
		assertEntryEquals(0, 4, sourceTracker, 101, targetTracker.getEntryAt(0));
		assertEntryEquals(4, 3, sourceTracker2, 200, targetTracker.getEntryAt(4));
	}
	
	@Test
	public void testTransitiveEndUnknown() {
		// set middleman to 100,101,102
		Tracker.setSource(middleman, 0, 3, source, 100);
		// set target to unknown,100,101,102,unknown,unknown
		Tracker.setSource(target, 1, 5, middleman, 0);
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		
		assertEquals(1, targetTracker.getEntryCount());
		assertEntryEquals(1, 3, sourceTracker, 100, targetTracker.getEntryAt(1));
	}
	
	@Test
	public void testTransitiveStartUnknown() {
		// set middleman to unknown,unknown,100,101,102
		Tracker.setSource(middleman, 2, 3, source, 100);
		// set target to unknown,unknown,unknown,100,101
		Tracker.setSource(target, 1, 4, middleman, 0);
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		
		assertEquals(1, targetTracker.getEntryCount());
		assertEntryEquals(3, 2, sourceTracker, 100, targetTracker.getEntryAt(3));
	}
	
	/** Take only a part of a part of the source of the source */
	@Test
	public void testTransitiveSinglePartlyPart() {
		// set middleman to 100,101,102,103,104,105,106,107,108,109,110,111,112
		Tracker.setSource(middleman, 0, 12, source, 100);
		// set target to 102,103,104,105
		Tracker.setSource(target, 1000, 4, middleman, 2);
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		
		assertEquals(1, targetTracker.getEntryCount());
		assertEntryEquals(1000, 4, sourceTracker, 102, targetTracker.getEntryAt(1000));
	}
	
	/** Make sure entries existing before and after the one involved don't break things */
	@Test
	public void testTransitiveIgnoreBeforeAndAfter() {
		Tracker.setSource(middleman, 0, 1, source, 100);
		Tracker.setSource(middleman, 1, 1, source, 200);
		Tracker.setSource(middleman, 2, 1, source, 300);
		Tracker.setSource(middleman, 3, 1, source, 400);
		Tracker.setSource(middleman, 4, 1, source, 500);
		
		//set target to 200,300,400
		Tracker.setSource(target, 1000, 3, middleman, 1);
		
		Tracker targetTracker = TrackerRepository.getTracker(target);
		assertEquals(3, targetTracker.getEntryCount());
		assertEntryEquals(1000, 1, sourceTracker, 200, targetTracker.getEntryAt(1000));
		assertEntryEquals(1001, 1, sourceTracker, 300, targetTracker.getEntryAt(1001));
		assertEntryEquals(1002, 1, sourceTracker, 400, targetTracker.getEntryAt(1002));
	}
	
	private void assertEntryEquals(int expectedEntryIndex, int expectedLength,
			Tracker expectedTracker, int expectedPartIndex, Entry<Integer, PartTracker> entry) {
		assertNotNull(entry);
		assertEquals((Integer) expectedEntryIndex, entry.getKey());
		
		PartTracker part = entry.getValue();
		assertNotNull(part);
		assertEquals(expectedLength, part.getLength());
		assertSame(expectedTracker, part.getTracker());
		assertEquals(expectedPartIndex, part.getIndex());
	}
}
