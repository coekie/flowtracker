package be.coekaerts.wouter.flowtracker.test;

import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;

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
		snapshotBuilder().track(abcdef, 3, 3).track(abcdef, 0, 3).assertTrackerOf(defabc);
	}

  // IntelliJ and maven/surefire replace System.out and System.err with their own implementations,
  // and we haven't implemented anything yet to handle those, so we don't test those.
}
