package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedString;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.Part;
import org.junit.Test;

public class StringTest {
  @Test public void testUnknown() {
    String a = untrackedString("unknownTest");
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

  @Test public void testCharAt() {
    String abc = trackCopy("abc");
    FlowTester.assertTrackedValue(abc.charAt(1), 'b', getStringTracker(abc), 1);
  }

  @Test public void testGetBytes() {
    String foobar = trackCopy("foobar");
    snapshotBuilder().trackString(foobar, 0, 6)
        .assertTrackerOf(foobar.getBytes());
  }

  @Test public void testGetChars() {
    String foobar = trackCopy("foobar");
    char[] dst = new char[6];
    foobar.getChars(3, 6, dst, 1);
    snapshotBuilder().gap(1).trackString(foobar, 3, 3)
        .assertTrackerOf(dst);
    // TODO test UTF-16 version
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
  @Test public void testReplaceChars() {
    String src = trackCopy("abc");
    FlowTester replacementCharTester = new FlowTester();
    String result = src.replace('b', replacementCharTester.createSourceChar('x'));
    assertEquals("axc", result);
    snapshotBuilder().trackString(src, 0, 1)
        .part(replacementCharTester.theSource(), replacementCharTester.theSourceIndex(), 1)
        .trackString(src, 2, 1)
        .assertEquals(getStringTracker(result));
  }

  /**
   * Test {@link String#replace(CharSequence, CharSequence)} with single-char String.
   * Note that {@link String#replace(CharSequence, CharSequence)} handles this single-char
   * replacement special.
   */
  @Test public void testReplaceSingleCharString() {
    String src = trackCopy("abc");
    String replacement = trackCopy("x");
    String result = src.replace("b", replacement);
    assertEquals("axc", result);
    snapshotBuilder().trackString(src, 0, 1).trackString(replacement).trackString(src, 2, 1)
        .assertEquals(getStringTracker(result));
  }

  @Test public void testStringConstant() {
    String str = ldcString();
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
    assertEquals(1, snapshot.getParts().size());
    Part part = snapshot.getParts().get(0);
    ClassOriginTracker sourceTracker = (ClassOriginTracker) part.source;
    assertEquals("test-ldc", sourceTracker.getContent()
        .subSequence(part.sourceIndex, part.sourceIndex + part.length));
    assertSame(str, ldcString());
  }

  // this is a separate method, so that there's only one entry for this in the constant pool for
  // constant-dynamic, so that the "assertSame" check above passes.
  // in instrumented code; a constant String referenced in two different places in the code is not
  // always the same instance (our instrumentation breaks String interning).
  private String ldcString() {
    return "test-ldc";
  }

  @SuppressWarnings("EqualsWithItself")
  @Test public void testStringConstantNoCondy() {
    String str = NoCondy.ldcNoCondy();
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
    assertEquals(1, snapshot.getParts().size());
    Part part = snapshot.getParts().get(0);
    ClassOriginTracker sourceTracker = (ClassOriginTracker) part.source;
    assertEquals("test-ldc-no-condy", sourceTracker.getContent()
        .subSequence(part.sourceIndex, part.sourceIndex + part.length));
    // would have been nice if they were, but currently multiple invocations do not return the same
    // instance.
    assertNotSame(NoCondy.ldcNoCondy(), NoCondy.ldcNoCondy());
  }

  /**
   * Tests if String equality (==) still kinda works, even though we broke interning.
   * Yeah, this is weird.
   */
  @SuppressWarnings("all")
  @Test public void testStringEquality() {
    String foo1 = "foo";
    String foo2 = "foo";
    assertTrue(foo1 == foo2);
    assertFalse(foo1 != foo2);
    assertNotSame(foo1, foo2);
  }

  // this class is excluded from using constant-dynamic, to be able to test instrumentation where
  // we cannot apply constant-dynamic.
  static class NoCondy {
    static String ldcNoCondy() {
      return "test-ldc-no-condy";
    }
  }
}
