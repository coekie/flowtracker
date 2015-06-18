package be.coekaerts.wouter.flowtracker.test;

import org.junit.Ignore;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("StringBufferReplaceableByString")
public class StringBuilderTest {
  @Test public void testAppendStuff() {
    String abc = trackCopy("abc");
    String def = trackCopy("def");
    String ghi = trackCopy("ghi");

    StringBuilder sb = new StringBuilder();
    sb.append(abc).append(def).append(ghi);
    String result = sb.toString();
    assertEquals("abcdefghi", result);

    snapshotBuilder().trackString(abc).trackString(def).trackString(ghi)
        .assertEquals(getStringTracker(result));
  }

  @Test public void testInsert() {
    String ab = trackCopy("ab");
    String cd = trackCopy("cd");
    String xyz = trackCopy("xyz");

    StringBuilder sb = new StringBuilder();
    sb.append(ab).append(cd);
    sb.insert(2, xyz);
    String result = sb.toString();
    assertEquals("abxyzcd", result);

    snapshotBuilder().trackString(ab).trackString(xyz).trackString(cd)
        .assertEquals(getStringTracker(result));
  }

  /** Use StringBuilder.insert to split the original value in two */
  @Test
  public void testInsertSplit() {
    String abcd = trackCopy("abcd");
    String xyz = trackCopy("xyz");

    StringBuilder sb = new StringBuilder();
    sb.append(abcd);
    sb.insert(2, xyz);
    String result = sb.toString();
    assertEquals("abxyzcd", result);

    snapshotBuilder().trackString(abcd, 0, 2).trackString(xyz).trackString(abcd, 2, 2)
        .assertEquals(getStringTracker(result));
  }

  @Test
  @Ignore("getting value from array not implemented")
  public void testReverse() {
    String abcd = trackCopy("abcd");
    StringBuilder sb = new StringBuilder(abcd);
    sb.reverse();
    String result = sb.toString();
    assertEquals("dcba", result);
    snapshotBuilder().trackString(abcd, 3, 1).trackString(abcd, 2, 1).trackString(abcd, 1, 1)
        .trackString(abcd, 0, 1)
        .assertEquals(getStringTracker(result));
  }
}
