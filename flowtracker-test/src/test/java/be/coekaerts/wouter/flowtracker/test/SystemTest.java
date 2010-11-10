package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;

import org.junit.Test;

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
}
