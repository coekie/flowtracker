package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;

/** Test FlowAnalyzingTransformer and friends */
@SuppressWarnings("StringBufferMayBeStringBuilder")
public class CharFlowAnalysisTest {
  private final FlowTester ft = new FlowTester();

  @Test public void charArrayLoadValue() {
    char[] array = TrackTestHelper.trackedCharArrayWithLength(3);
    FlowTester.assertTrackedValue(array[2], getTracker(array), 2);
  }

  @Test public void charArrayStore() {
    char[] array = new char[3];
    array[2] = ft.createSourceChar('a');
    snapshotBuilder().gap(2).part(ft.theSource(), ft.theSourceIndex(), 1)
        .assertTrackerOf(array);
  }

  @Test public void charArrayLoadAndStore() {
    char[] abc = TrackTestHelper.trackedCharArrayWithLength(3);

    char[] array = new char[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    snapshotBuilder().track(abc, 1, 1).track(abc, 0, 1).track(abc, 2, 1)
        .assertTrackerOf(array);
  }

  @Test public void byteArray() {
    byte[] abc = trackedByteArray("abc");

    byte[] array = new byte[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    snapshotBuilder().track(abc, 1, 1).track(abc, 0, 1).track(abc, 2, 1)
        .assertTrackerOf(array);
  }

  @Test public void charAt() {
    String abc = trackCopy("abc");
    FlowTester.assertTrackedValue(abc.charAt(1), getStringTracker(abc), 1);
  }

  @SuppressWarnings("UnnecessaryLocalVariable") // we want its type to be a CharSequence
  @Test public void charSequenceCharAt() {
    String str = trackCopy("abc");
    CharSequence abc = str;

    FlowTester.assertTrackedValue(abc.charAt(1), getStringTracker(str), 1);
  }

  @Test public void stringBuilderAppendChar() {
    String abc = trackCopy("abc");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < abc.length(); i++) {
      sb.append(abc.charAt(i));
    }
    String result = sb.toString();

    assertEquals("abc", result);

    snapshotBuilder().trackString(abc, 0, 3)
        .assertEquals(getStringTracker(result));
  }

  @Test public void stringBufferAppendChar() {
    String abc = trackCopy("abc");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < abc.length(); i++) {
      sb.append(abc.charAt(i));
    }
    String result = sb.toString();

    assertEquals("abc", result);

    snapshotBuilder().trackString(abc, 0, 3)
        .assertEquals(getStringTracker(result));
  }

  // This one is hard.
  // Both assignments into array come from the same statement,
  // but "secondLast" does not contain the *last* execution of that statement anymore.
  // Ideally, we would follow the flow of these local variables; but that's not so important now.
  // But we must detect this, and mark the origin as unknown.
  @Test public void charAtFlow() {
    String abc = trackCopy("abc");

    char[] array = new char[2];

    char secondLast = 0;
    char last = 0;

    for (int i = 0; i < 2; i++) {
      secondLast = last;
      last = abc.charAt(i);
    }

    array[0] = secondLast;
    array[1] = last;

    assertNull(getTracker(array));
    // or, it would be nicer if: snapshotBuilder().stringPart(abc, 0, 2).assertTrackerOf(array)
  }

  // we store the origin of a value before we actually call the method,
  // so what happens if it throws an exception...
  @Test public void charAtException() {
    String abc = trackCopy("abc");

    char[] array = new char[10];

    char x = 0;

    try {
      x = abc.charAt(1000);
    } catch (IndexOutOfBoundsException ignore) {
    }
    array[0] = x;

    // we notice that it's not an easy case, so we don't track it
    assertNull(getTracker(array));
  }

  // TODO charToByte for Latin1String.inflate
  //  - TrackableValue for constants?
  //  - track through "&" operation and cast
  @Ignore
  @Test public void charToByte() {
    byte[] src = trackedByteArray("abcd");
    char[] dst = new char[3];
    for (int i = 0; i < 3; i++) {
      // test case from Latin1String.inflate
      dst[i] = (char)(src[i + 1] & 255);
    }

    snapshotBuilder().track(src, 1, 3).assertTrackerOf(dst);
  }
}
