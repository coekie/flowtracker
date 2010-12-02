package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertStringOriginPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.strPart;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class StringBuilderTest {
	@Test
	public void testAppendStuff() {
		String abc = trackCopy("abc");
		String def = trackCopy("def");
		String ghi = trackCopy("ghi");
		
		StringBuilder sb = new StringBuilder();
		sb.append(abc).append(def).append(ghi);
		String result = sb.toString();
		assertEquals("abcdefghi", result);
		
		assertStringOriginPartsCompleteEqual(result, strPart(abc), strPart(def), strPart(ghi));
	}
	
	@Test
	public void testInsert() {
		String ab = trackCopy("ab");
		String cd = trackCopy("cd");
		String xyz = trackCopy("xyz");
		
		StringBuilder sb = new StringBuilder();
		sb.append(ab).append(cd);
		sb.insert(2, xyz);
		String result = sb.toString();
		assertEquals("abxyzcd", result);
		
		assertStringOriginPartsCompleteEqual(result, strPart(ab), strPart(xyz), strPart(cd));
	}
	
	/**
	 * Use StringBuilder.insert to split the original value in two
	 */
	@Test
	@Ignore("splitting not implemented in Tracker")
	public void testInsertSplit() {
		String abcd = trackCopy("abcd");
		String xyz = trackCopy("xyz");
		
		StringBuilder sb = new StringBuilder();
		sb.append(abcd);
		sb.insert(2, xyz);
		String result = sb.toString();
		assertEquals("abxyzcd", result);
		
		assertStringOriginPartsCompleteEqual(result, strPart(abcd, 0, 2), strPart(xyz), strPart(abcd, 2, 2));
	}
	
	@Test
	@Ignore("getting value from array not implemented")
	public void testReverse() {
		String abcd = trackCopy("abcd");
		StringBuilder sb = new StringBuilder(abcd);
		sb.reverse();
		String result = sb.toString();
		assertEquals("dcba", result);
		assertStringOriginPartsCompleteEqual(result, strPart(abcd, 3, 1), strPart(abcd, 2, 1),
				strPart(abcd, 1, 1), strPart(abcd, 0, 1));
	}
}
