package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

@SuppressWarnings("StringBufferReplaceableByString")
public class StringBuilderTest {
  @Test public void testAppendString() {
    String abc = trackCopy("abc");
    String def = trackCopy("def");
    String ghi = trackCopy("ghi");

    StringBuilder sb = new StringBuilder();
    sb.append(abc).append(def).append(ghi);
    String result = sb.toString();
    assertThat(result).isEqualTo("abcdefghi");

    assertThatTracker(getStringTracker(result)).matches(
        snapshot().trackString(abc).trackString(def).trackString(ghi));
  }

  @Test public void testAppendCharSequenceRange() {
    String abcdef = trackCopy("abcdef");
    StringBuilder sb = new StringBuilder();
    sb.append(abcdef, 1, 4);
    String result = sb.toString();
    assertThat(result).isEqualTo("bcd");

    assertThatTracker(getStringTracker(result)).matches(snapshot().trackString(3, abcdef, 1));
  }

  @Test public void testInsert() {
    String ab = trackCopy("ab");
    String cd = trackCopy("cd");
    String xyz = trackCopy("xyz");

    StringBuilder sb = new StringBuilder();
    sb.append(ab).append(cd);
    sb.insert(2, xyz);
    String result = sb.toString();
    assertThat(result).isEqualTo("abxyzcd");

    assertThatTracker(getStringTracker(result)).matches(
        snapshot().trackString(ab).trackString(xyz).trackString(cd));
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
    assertThat(result).isEqualTo("abxyzcd");

    assertThatTracker(getStringTracker(result)).matches(
        snapshot().trackString(2, abcd, 0).trackString(xyz).trackString(2, abcd, 2));
  }

  @Test
  public void testReverse() {
    String abcd = trackCopy("abcd");
    StringBuilder sb = new StringBuilder(abcd);
    sb.reverse();
    String result = sb.toString();
    assertThat(result).isEqualTo("dcba");
    assertThatTracker(getStringTracker(result)).matches(snapshot()
        .trackString(1, abcd, 3).trackString(1, abcd, 2).trackString(1, abcd, 1)
        .trackString(1, abcd, 0));
  }

  @Test
  public void replace() {
    String abcd = trackCopy("abcdef");
    String x = trackCopy("x");
    StringBuilder sb = new StringBuilder(abcd);
    sb.replace(1, 3, x);
    String result = sb.toString();
    assertThat(result).isEqualTo("axdef");
    assertThatTracker(getStringTracker(result)).matches(snapshot()
        .trackString(1, abcd, 0).trackString(1, x, 0).trackString(3, abcd, 3));
  }
}
