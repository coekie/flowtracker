package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.StringHook;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import org.junit.Test;

@SuppressWarnings("StringBufferReplaceableByString")
public class StringBuilderTest {
  @Test public void testAppendString() {
    String abc = TrackTestHelper.trackCopy("abc");
    String def = TrackTestHelper.trackCopy("def");
    String ghi = TrackTestHelper.trackCopy("ghi");

    StringBuilder sb = new StringBuilder();
    sb.append(abc).append(def).append(ghi);
    String result = sb.toString();
    assertThat(result).isEqualTo("abcdefghi");

    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot().trackString(abc).trackString(def).trackString(ghi));
  }

  @Test public void testAppendCharSequenceRange() {
    String abcdef = TrackTestHelper.trackCopy("abcdef");
    StringBuilder sb = new StringBuilder();
    sb.append(abcdef, 1, 4);
    String result = sb.toString();
    assertThat(result).isEqualTo("bcd");

    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot().trackString(3, abcdef, 1));
  }

  @Test public void testInsert() {
    String ab = TrackTestHelper.trackCopy("ab");
    String cd = TrackTestHelper.trackCopy("cd");
    String xyz = TrackTestHelper.trackCopy("xyz");

    StringBuilder sb = new StringBuilder();
    sb.append(ab).append(cd);
    sb.insert(2, xyz);
    String result = sb.toString();
    assertThat(result).isEqualTo("abxyzcd");

    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot().trackString(ab).trackString(xyz).trackString(cd));
  }

  /** Use StringBuilder.insert to split the original value in two */
  @Test
  public void testInsertSplit() {
    String abcd = TrackTestHelper.trackCopy("abcd");
    String xyz = TrackTestHelper.trackCopy("xyz");

    StringBuilder sb = new StringBuilder();
    sb.append(abcd);
    sb.insert(2, xyz);
    String result = sb.toString();
    assertThat(result).isEqualTo("abxyzcd");

    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot().trackString(2, abcd, 0).trackString(xyz).trackString(2, abcd, 2));
  }

  @Test
  public void testReverse() {
    String abcd = TrackTestHelper.trackCopy("abcd");
    StringBuilder sb = new StringBuilder(abcd);
    sb.reverse();
    String result = sb.toString();
    assertThat(result).isEqualTo("dcba");
    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot()
        .trackString(1, abcd, 3).trackString(1, abcd, 2).trackString(1, abcd, 1)
        .trackString(1, abcd, 0));
  }

  @Test
  public void replace() {
    String abcd = TrackTestHelper.trackCopy("abcdef");
    String x = TrackTestHelper.trackCopy("x");
    StringBuilder sb = new StringBuilder(abcd);
    sb.replace(1, 3, x);
    String result = sb.toString();
    assertThat(result).isEqualTo("axdef");
    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot()
        .trackString(1, abcd, 0).trackString(1, x, 0).trackString(3, abcd, 3));
  }
}
