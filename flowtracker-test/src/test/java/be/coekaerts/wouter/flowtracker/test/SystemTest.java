package be.coekaerts.wouter.flowtracker.test;

import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;

/**
 * Test for {@link System}
 */
public class SystemTest {
	@Test
	public void charArrayCopy() {
		char[] abcdef = track("abcdef".toCharArray());
		char[] defabc = new char[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
		snapshotBuilder().track(abcdef, 3, 3).track(abcdef, 0, 3).assertTrackerOf(defabc);
		assertEquals('d', defabc[0]);
	}

	@Test
	public void byteArrayCopy() {
		byte[] abcdef = track(new byte[]{1, 2, 3, 4, 5, 6});
		byte[] defabc = new byte[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
		snapshotBuilder().track(abcdef, 3, 3).track(abcdef, 0, 3).assertTrackerOf(defabc);
		assertEquals(4, defabc[0]);
	}

  // IntelliJ and maven/surefire replace System.out and System.err with their own implementations,
  // and we haven't implemented anything yet to handle those, so we don't test those.
}
