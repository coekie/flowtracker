package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import junit.framework.Assert;

import org.junit.Test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

public class StringTest {
	@Test
	public void unkownTest() {
		String a = "unkownTest";
		Assert.assertNull(TrackerRepository.getTracker(a));
	}
	
	@Test
	public void concatTest() {
		String a = trackCopy("concatTest1");
		String b = trackCopy("concatTest2");
		String ab = a.concat(b);
		Assert.assertEquals("concatTest1concatTest2", ab);
		
		assertPartsCompleteEqual(ab, part(a), part(b));
	}
	
	@Test
	public void substringBeginTest()  {
		String foobar = trackCopy("foobar");
		String foo = foobar.substring(0, 3);
		Assert.assertEquals("foo", foo);
		
		assertPartsCompleteEqual(foo, part(foobar, 0, 3));
	}
	
	@Test
	public void substringEndTest()  {
		String foobar = trackCopy("foobar");
		String bar = foobar.substring(3);
		Assert.assertEquals("bar", bar);
		
		assertPartsCompleteEqual(bar, part(foobar, 3, 3));
	}
}
