package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import org.junit.Ignore;
import org.junit.Test;

public class StringTest {
  @Test public void testUnknown() {
    String a = "unknownTest";
    assertNull(TrackerRepository.getTracker(a));
    assertNull(getStringTracker(a));
  }

  @Test public void testToCharArray() {
    String foo = trackCopy("foo");
    char[] array = foo.toCharArray();
    snapshotBuilder().trackString(foo).assertEquals(TrackerRepository.getTracker(array));
  }

  @Test public void testConcat() {
    String foo = trackCopy("foo");
    String bar = trackCopy("bar");
    String foobar = foo.concat(bar);
    assertEquals("foobar", foobar);

    snapshotBuilder().trackString(foo).trackString(bar)
        .assertEquals(getStringTracker(foobar));
  }

  @Test public void testSubstringBegin() {
    String foobar = trackCopy("foobar");
    String foo = foobar.substring(0, 3);
    assertEquals("foo", foo);

    snapshotBuilder().trackString(foobar, 0, 3)
        .assertEquals(getStringTracker(foo));
  }

  @Test public void testSubstringEnd() {
    String foobar = trackCopy("foobar");
    String bar = foobar.substring(3);
    assertEquals("bar", bar);

    snapshotBuilder().trackString(foobar, 3, 3)
        .assertEquals(getStringTracker(bar));
  }

  @Test public void testGetStringTracker() {
    char[] chars = trackedCharArray("abcd");
    String str = new String(chars, 1, 2); // create String "bc"
    assertEquals("bc", str);

    snapshotBuilder().track(chars, 1, 2)
        .assertEquals(getStringTracker(str));
  }

  /** Test {@link String#replace(CharSequence, CharSequence)} */
  @Test public void testReplaceStrings() {
    String src = trackCopy("abcd");
    String replacement = trackCopy("x");
    String result = src.replace("bc", replacement);
    assertEquals("axd", result);
    snapshotBuilder().trackString(src, 0, 1).trackString(replacement).trackString(src, 3, 1)
        .assertEquals(getStringTracker(result));
  }

  /** Test {@link String#replace(char, char)} */
  // TODO flow analysis is not smart enough yet for StringLatin1.replace.
  //   probably because of this line: buf[i] = (c == (byte)oldChar) ? (byte)newChar : c;
  //   it can't track where value came from if that depends on what code path it took?
  @Ignore
  @Test public void testReplaceChars() {
    String src = trackCopy("abc");
    String result = src.replace('b', 'x');
    assertEquals("axc", result);
    snapshotBuilder().trackString(src, 0, 1).gap(1).trackString(src, 2, 1)
        .assertEquals(getStringTracker(result));
  }

  /** Test {@link String#replace(CharSequence, CharSequence)} with single-char String */
  // String.replace handles replacing single-char String with another single-char String special
  @Ignore // same as testReplaceChars()
  @Test public void testReplaceSingleCharString() {
    String src = trackCopy("abc");
    String replacement = trackCopy("x");
    String result = src.replace("b", replacement);
    assertEquals("axc", result);
    snapshotBuilder().trackString(src, 0, 1).trackString(replacement).trackString(src, 2, 1)
        .assertEquals(getStringTracker(result));
  }
}
