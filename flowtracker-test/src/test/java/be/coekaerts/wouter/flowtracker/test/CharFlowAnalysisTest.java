package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Test FlowAnalyzingTransformer and friends */
@SuppressWarnings("StringBufferMayBeStringBuilder")
public class CharFlowAnalysisTest {
  @Test public void charAt() {
    String abc = trackCopy("abc");

    char[] array = new char[3];
    array[0] = abc.charAt(1);
    array[1] = abc.charAt(0);
    array[2] = abc.charAt(2);

    snapshotBuilder().trackString(abc, 1, 1).trackString(abc, 0, 1).trackString(abc, 2, 1)
        .assertTrackerOf(array);
  }

  @Test public void stringBuilderAppend() {
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

  @Test public void stringBufferAppend() {
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
  // but x does not contain the *last* execution of that statement anymore.
  // Optimally, we should follow the flow of these local variables.
  // Or we should at least detect this, and mark the origin as unknown.
  @Test public void charAtFlow() {
    String abc = trackCopy("abc");

    char[] array = new char[2];

    char x = 0;
    char y = 0;

    for (int i = 0; i < 2; i++) {
      x = y;
      y = abc.charAt(i);
    }

    array[0] = x;
    array[1] = y;

    assertNull(TrackerRepository.getTracker(array));
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
    assertNull(TrackerRepository.getTracker(array));
  }
}
