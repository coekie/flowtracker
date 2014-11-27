package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static org.junit.Assert.assertEquals;

/**
 * Test for {@link System}
 */
public class SystemTest {
	@Test
	public void arraycopy() {
		char[] abcdef = track("abcdef".toCharArray());
		char[] defabc = new char[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
		assertPartsCompleteEqual(defabc, part(abcdef, 3, 3), part(abcdef, 0, 3));
	}

  @Test public void inOutErr() {
    // IntelliJ and maven/surefire replace System.out and System.err with their own implementations,
    // and we haven't implement anything yet to handle those, so we don't test those.
    assertEquals("System.in", TrackerRepository.getTracker(System.in).getDescriptor());
  }
}
