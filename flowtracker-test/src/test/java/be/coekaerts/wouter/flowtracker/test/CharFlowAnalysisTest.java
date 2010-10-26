package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;

import org.junit.Assert;
import org.junit.Test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

public class CharFlowAnalysisTest {
	@Test
	public void charAt() {
		String abc = trackCopy("abc");
		
		char[] array = new char[3];
		array[0] = abc.charAt(1);
		array[1] = abc.charAt(0);
		array[2] = abc.charAt(2);
		
		assertPartsCompleteEqual(array, part(abc, 1, 1), part(abc, 0, 1), part(abc, 2, 1));
	}
	
	// This one is hard.
	// Both assignments into array come from the same statement,
	// but x does not contain the *last* execution of that statement anymore.
	// Optimally, we should follow the flow of these local variables.
	// Or we should at least detect this, and mark the origin as unknown.
	@Test
	public void charAtFlow() {
		String abc = trackCopy("abc");
		
		char[] array = new char[2];
		
		char x = 0;
		char y = 0;
		
		for (int i = 0; i < 2; i++) {
			x = y;
			y = abc.charAt(i);
		}
		
		array[0] = x;
		array[1] = y;
		
		Assert.assertNull(TrackerRepository.getTracker(array));
		// or, it would be nicer if: assertPartsCompleteEqual(array, part(abc, 0, 2));
	}
	
	// we store the origin of a value before we actually call the method,
	// so what happens if it throws an exception...
	@Test
	public void charAtException() {
		String abc = trackCopy("abc");
		
		char[] array = new char[10];
		
		char x = 0;
		
		try {
			x = abc.charAt(1000);
		} catch (IndexOutOfBoundsException e) {
		}
		array[0] = x;
		
		// we notice that it's not an easy case, so we don't track it
		Assert.assertNull(TrackerRepository.getTracker(array));
	}
}
