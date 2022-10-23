package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test for {@link System}
 */
public class SystemTest {
	/** Not actually a test, but helps with debugging maven toolchain stuff */
	@Test public void printJavaVersion() {
		System.out.println("Java version: " + System.getProperty("java.version"));
	}

	@Test
	public void charArrayCopy() {
		char[] abcdef = trackedCharArray("abcdef");
		char[] defabc = new char[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
		snapshotBuilder().track(abcdef, 3, 3).track(abcdef, 0, 3).assertTrackerOf(defabc);
		assertEquals('d', defabc[0]);
	}

	@Test
	public void byteArrayCopy() {
		byte[] abcdef = trackedByteArray("abcdef");
		byte[] defabc = new byte[6];
		System.arraycopy(abcdef, 0, defabc, 3, 3); // copy abc
		System.arraycopy(abcdef, 3, defabc, 0, 3); // copy def
		snapshotBuilder().track(abcdef, 3, 3).track(abcdef, 0, 3).assertTrackerOf(defabc);
		assertEquals('d', defabc[0]);
	}

  // IntelliJ and maven/surefire replace System.out and System.err with their own implementations,
  // and we haven't implemented anything yet to handle those, so we don't test those.
}
