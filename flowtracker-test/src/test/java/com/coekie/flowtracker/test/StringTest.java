package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertIsFallbackIn;
import static com.coekie.flowtracker.test.TrackTestHelper.getClassOriginTrackerContent;
import static com.coekie.flowtracker.test.TrackTestHelper.stringTracker;
import static com.coekie.flowtracker.test.TrackTestHelper.trackCopy;
import static com.coekie.flowtracker.test.TrackTestHelper.trackedCharArray;
import static com.coekie.flowtracker.test.TrackTestHelper.untrackedString;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.StringConcatFactoryHook;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.lang.invoke.StringConcatFactory;
import org.junit.Test;

public class StringTest {
  @Test public void testUnknown() {
    String a = untrackedString("unknownTest");
    assertThat(TrackerRepository.getTracker(context(), a)).isNull();
    assertThat(stringTracker(a)).isNull();
  }

  @Test public void testToCharArray() {
    String foo = trackCopy("foo");
    char[] array = foo.toCharArray();
    assertThatTrackerOf(array).matches(snapshot().trackString(foo));
  }

  @Test public void testConcat() {
    String foo = trackCopy("foo");
    String bar = trackCopy("bar");
    String foobar = foo.concat(bar);
    assertThat(foobar).isEqualTo("foobar");

    assertThatTracker(stringTracker(foobar)).matches(
        snapshot().trackString(foo).trackString(bar));
  }

  @Test public void testSubstringBegin() {
    String foobar = trackCopy("foobar");
    String foo = foobar.substring(0, 3);
    assertThat(foo).isEqualTo("foo");

    assertThatTracker(stringTracker(foo)).matches(snapshot().trackString(3, foobar, 0));
  }

  @Test public void testSubstringEnd() {
    String foobar = trackCopy("foobar");
    String bar = foobar.substring(3);
    assertThat(bar).isEqualTo("bar");

    assertThatTracker(stringTracker(bar)).matches(snapshot().trackString(3, foobar, 3));
  }

  @Test public void testCharAt() {
    String abc = trackCopy("abc");
    FlowTester.assertTrackedValue(abc.charAt(1), 'b', stringTracker(abc), 1);
  }

  @Test
  public void testCharAt_UTF16() {
    String abc = trackCopy("abc\u0939");
    TrackerPoint point = FlowTester.getCharSourcePoint(abc.charAt(1));
    assertThat(point.tracker).isEqualTo(stringTracker(abc));
    assertThat(point.index).isEqualTo(2);
    assertThat(point.length).isEqualTo(2);
  }

  @SuppressWarnings("UnnecessaryLocalVariable") // we want its type to be a CharSequence
  @Test
  public void testCharAt_CharSequence_String() {
    String str = trackCopy("abc");
    CharSequence abc = str;

    FlowTester.assertTrackedValue(abc.charAt(1), 'b', stringTracker(str), 1);
  }

  @SuppressWarnings("UnnecessaryLocalVariable") // we want its type to be a CharSequence
  @Test
  public void testCharAt_CharSequence_NonString() {
    String str = trackCopy("abc");
    StringBuilder sb = new StringBuilder(str);
    CharSequence abc = sb;

    FlowTester.assertTrackedValue(abc.charAt(1), 'b', stringTracker(str), 1);
  }

  @Test public void testGetBytes() {
    String foobar = trackCopy("foobar");
    assertThatTrackerOf(foobar.getBytes()).matches(snapshot().trackString(6, foobar, 0));
  }

  @Test public void testGetChars() {
    String foobar = trackCopy("foobar");
    char[] dst = new char[6];
    foobar.getChars(3, 6, dst, 1);
    assertThatTrackerOf(dst).matches(snapshot().gap(1).trackString(3, foobar, 3));
    // TODO test UTF-16 version
  }

  @Test public void testGetStringTracker() {
    char[] chars = trackedCharArray("abcd");
    String str = new String(chars, 1, 2); // create String "bc"
    assertThat(str).isEqualTo("bc");

    assertThatTracker(stringTracker(str)).matches(snapshot().track(2, chars, 1));
  }

  /** Test {@link String#replace(CharSequence, CharSequence)} */
  @Test public void testReplaceStrings() {
    String src = trackCopy("abcd");
    String replacement = trackCopy("x");
    String result = src.replace("bc", replacement);
    assertThat(result).isEqualTo("axd");
    assertThatTracker(stringTracker(result)).matches(
        snapshot().trackString(1, src, 0).trackString(replacement).trackString(1, src, 3));
  }

  /** Test {@link String#replace(char, char)} */
  @Test public void testReplaceChars() {
    String src = trackCopy("abc");
    FlowTester replacementCharTester = new FlowTester();
    String result = src.replace('b', replacementCharTester.createSourceChar('x'));
    assertThat(result).isEqualTo("axc");
    assertThatTracker(stringTracker(result)).matches(snapshot().trackString(1, src, 0)
        .part(replacementCharTester.point())
        .trackString(1, src, 2));
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
    assertThat(result).isEqualTo("axc");
    assertThatTracker(stringTracker(result)).matches(
        snapshot().trackString(1, src, 0).trackString(replacement).trackString(1, src, 2));
  }

  @Test public void testStringConstant() {
    String str = ldcString();
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("test-ldc");
    assertThat(ldcString()).isSameInstanceAs(str);
  }

  // this is a separate method, so that there's only one entry for this in the constant pool for
  // constant-dynamic, so that the "isSameInstanceAs" check above passes.
  // in instrumented code; a constant String referenced in two different places in the code is not
  // always the same instance (our instrumentation breaks String interning).
  private String ldcString() {
    return "test-ldc";
  }

  @Test public void testStringConstantNoCondy() {
    String str = NoCondy.ldcNoCondy();
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("test-ldc-no-condy");
    // would have been nice if they were, but currently multiple invocations do not return the same
    // instance.
    assertThat(NoCondy.ldcNoCondy()).isNotSameInstanceAs(NoCondy.ldcNoCondy());
  }

  /**
   * Tests if String equality (==) still kinda works, even though we broke interning.
   * Yeah, this is weird.
   */
  @SuppressWarnings("all")
  @Test public void testStringEquality() {
    String foo1 = "foo";
    String foo2 = "foo";
    assertThat(foo1 == foo2).isTrue(); // NOT same as assertThat(foo1).isSameInstanceAs(foo2)!
    assertThat(foo1 != foo2).isFalse();
    assertThat(foo1).isNotSameInstanceAs(foo2);
  }

  // this class is excluded from using constant-dynamic, to be able to test instrumentation where
  // we cannot apply constant-dynamic.
  static class NoCondy {
    static String ldcNoCondy() {
      return "test-ldc-no-condy";
    }
  }

  /**
   * Test tracking of String literals in concatenation, that is tracking of the recipe ("(\1)")
   * in {@link StringConcatFactory}.
   * This case does *not* go through {@link StringConcatFactoryHook}.
   */
  @Test
  public void testStringConcatFactory() {
    int i = 1;
    String str = "(" + i + ")";
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(3);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("(");
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(2)))
        .isEqualTo(")");
  }

  /** Test {@link StringConcatFactoryHook} */
  @Test
  public void testStringConcatFactoryWithCharLiterals() {
    FlowTester a = new FlowTester();
    FlowTester b = new FlowTester();
    int i = 0; // a parameter in between that's not tracked
    String str = "(" + a.createSourceChar('a') + "," + i + "," + b.createSourceChar('b') + ")";
    assertThat(str).isEqualTo("(a,0,b)");
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(7);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("(");
    assertThat(snapshot.getParts().get(1).source).isSameInstanceAs(a.tracker());
    assertThat(snapshot.getParts().get(1).sourceIndex).isEqualTo(a.index());
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(2)))
        .isEqualTo(",");
    if (Runtime.version().feature() < 22) {
      assertThat(snapshot.getParts().get(3).source).isNull();
    } else {
      // due to some changes, since jdk 22, we see it as coming from the StringLatin1.getChars
      // method that converts integers to strings.
      // that isn't exactly ideal, but good enough for now.
      // (perhaps we should do something that maps it to a String earlier, so it becomes a fallback
      //  pointing to the location of the string concatenation).
      assertIsFallbackIn(snapshot.getParts().get(3), "java.lang.StringLatin1");
    }
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(4)))
        .isEqualTo(",");
    assertThat(snapshot.getParts().get(5).source).isSameInstanceAs(b.tracker());
    assertThat(snapshot.getParts().get(5).sourceIndex).isEqualTo(b.index());
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(6)))
        .isEqualTo(")");
  }
}
