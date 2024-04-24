package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedString;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.hook.StringConcatFactoryHook;
import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.Part;
import java.lang.invoke.StringConcatFactory;
import org.junit.Test;

public class StringTest {
  @Test public void testUnknown() {
    String a = untrackedString("unknownTest");
    assertThat(TrackerRepository.getTracker(a)).isNull();
    assertThat(getStringTracker(a)).isNull();
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

    assertThatTracker(getStringTracker(foobar)).matches(
        snapshot().trackString(foo).trackString(bar));
  }

  @Test public void testSubstringBegin() {
    String foobar = trackCopy("foobar");
    String foo = foobar.substring(0, 3);
    assertThat(foo).isEqualTo("foo");

    assertThatTracker(getStringTracker(foo)).matches(snapshot().trackString(foobar, 0, 3));
  }

  @Test public void testSubstringEnd() {
    String foobar = trackCopy("foobar");
    String bar = foobar.substring(3);
    assertThat(bar).isEqualTo("bar");

    assertThatTracker(getStringTracker(bar)).matches(snapshot().trackString(foobar, 3, 3));
  }

  @Test public void testCharAt() {
    String abc = trackCopy("abc");
    FlowTester.assertTrackedValue(abc.charAt(1), 'b', getStringTracker(abc), 1);
  }

  @Test public void testGetBytes() {
    String foobar = trackCopy("foobar");
    assertThatTrackerOf(foobar.getBytes()).matches(snapshot().trackString(foobar, 0, 6));
  }

  @Test public void testGetChars() {
    String foobar = trackCopy("foobar");
    char[] dst = new char[6];
    foobar.getChars(3, 6, dst, 1);
    assertThatTrackerOf(dst).matches(snapshot().gap(1).trackString(foobar, 3, 3));
    // TODO test UTF-16 version
  }

  @Test public void testGetStringTracker() {
    char[] chars = trackedCharArray("abcd");
    String str = new String(chars, 1, 2); // create String "bc"
    assertThat(str).isEqualTo("bc");

    assertThatTracker(getStringTracker(str)).matches(snapshot().track(chars, 1, 2));
  }

  /** Test {@link String#replace(CharSequence, CharSequence)} */
  @Test public void testReplaceStrings() {
    String src = trackCopy("abcd");
    String replacement = trackCopy("x");
    String result = src.replace("bc", replacement);
    assertThat(result).isEqualTo("axd");
    assertThatTracker(getStringTracker(result)).matches(
        snapshot().trackString(src, 0, 1).trackString(replacement).trackString(src, 3, 1));
  }

  /** Test {@link String#replace(char, char)} */
  @Test public void testReplaceChars() {
    String src = trackCopy("abc");
    FlowTester replacementCharTester = new FlowTester();
    String result = src.replace('b', replacementCharTester.createSourceChar('x'));
    assertThat(result).isEqualTo("axc");
    assertThatTracker(getStringTracker(result)).matches(snapshot().trackString(src, 0, 1)
        .part(replacementCharTester.theSource(), replacementCharTester.theSourceIndex(), 1)
        .trackString(src, 2, 1));
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
    assertThatTracker(getStringTracker(result)).matches(
        snapshot().trackString(src, 0, 1).trackString(replacement).trackString(src, 2, 1));
  }

  @Test public void testStringConstant() {
    String str = ldcString();
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    Part part = snapshot.getParts().get(0);
    ClassOriginTracker sourceTracker = (ClassOriginTracker) part.source;
    assertThat(sourceTracker.getContent()
        .subSequence(part.sourceIndex, part.sourceIndex + part.length))
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
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("test-ldc-no-condy");
    // would have been nice if they were, but currently multiple invocations do not return the same
    // instance.
    assertThat(NoCondy.ldcNoCondy()).isNotSameInstanceAs(NoCondy.ldcNoCondy());
  }

  /**
   * Extract the contents of the ClassOriginTracker that the give part points to. This is to
   * validate that the source that the part points to really contains the expected value.
   */
  private String getClassOriginTrackerContent(TrackerSnapshot.Part part) {
    assertThat(part.source).isInstanceOf(ClassOriginTracker.class);
    ClassOriginTracker sourceTracker = (ClassOriginTracker) part.source;
    return sourceTracker.getContent()
        .subSequence(part.sourceIndex, part.sourceIndex + part.length)
        .toString();
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
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
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
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
    assertThat(snapshot.getParts()).hasSize(7);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("(");
    assertThat(snapshot.getParts().get(1).source).isSameInstanceAs(a.theSource());
    assertThat(snapshot.getParts().get(1).sourceIndex).isEqualTo(a.theSourceIndex());
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(2)))
        .isEqualTo(",");
    assertThat(snapshot.getParts().get(3).source).isNull();
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(4)))
        .isEqualTo(",");
    assertThat(snapshot.getParts().get(5).source).isSameInstanceAs(b.theSource());
    assertThat(snapshot.getParts().get(5).sourceIndex).isEqualTo(b.theSourceIndex());
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(6)))
        .isEqualTo(")");
  }
}
