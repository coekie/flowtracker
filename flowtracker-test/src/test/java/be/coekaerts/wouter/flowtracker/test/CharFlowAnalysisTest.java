package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test FlowAnalyzingTransformer and friends */
@SuppressWarnings("StringBufferMayBeStringBuilder")
public class CharFlowAnalysisTest {
  private final FlowTester ft = new FlowTester();

  @Test public void charArrayLoadValue() {
    char[] array = TrackTestHelper.trackedCharArrayWithLength(3);
    FlowTester.assertTrackedValue(array[2], '\0', getTracker(array), 2);
  }

  @Test public void charArrayStore() {
    char[] array = new char[3];
    array[2] = ft.createSourceChar('a');
    assertThatTrackerOf(array).matches(
        snapshot().gap(2).part(ft.theSource(), ft.theSourceIndex(), 1));
  }

  @Test public void charArrayLoadAndStore() {
    char[] abc = TrackTestHelper.trackedCharArrayWithLength(3);

    char[] array = new char[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    assertThatTrackerOf(array).matches(
        snapshot().track(abc, 1, 1).track(abc, 0, 1).track(abc, 2, 1));
  }

  @Test public void byteArrayLoadAndStore() {
    byte[] abc = trackedByteArray("abc");

    byte[] array = new byte[3];
    array[0] = abc[1];
    array[1] = abc[0];
    array[2] = abc[2];

    assertThatTrackerOf(array).matches(
        snapshot().track(abc, 1, 1).track(abc, 0, 1).track(abc, 2, 1));
  }

  @Test public void charArrayClone() {
    char[] array = TrackTestHelper.trackedCharArrayWithLength(3);
    assertThatTrackerOf(array.clone()).matches(snapshot().track(array));
  }

  @Test public void byteArrayClone() {
    byte[] array = trackedByteArray("abc");
    assertThatTrackerOf(array.clone()).matches(snapshot().track(array));
  }

  // regression test for NPE in analysis when byte[] type is null
  @SuppressWarnings("ConstantValue")
  @Test public void byteArrayNull() {
    byte[] a = trackedByteArray("a");

    byte[] bytes = null;
    if (bytes == null) {
      bytes = new byte[1];
    }
    bytes[0] = a[0];

    // would be nice if this was tracked. for now, we're happy with it not blowing up
    // snapshotBuilder().track(a, 0, 1).assertTrackerOf(bytes);
    assertThat(getTracker(bytes)).isNull();
  }

  @Test public void stringBuilderAppendChar() {
    String abc = trackCopy("abc");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < abc.length(); i++) {
      sb.append(abc.charAt(i));
    }
    String result = sb.toString();

    assertThat(result).isEqualTo("abc");

    assertThatTracker(getStringTracker(result)).matches(snapshot().trackString(abc, 0, 3));
  }

  @Test public void stringBufferAppendChar() {
    String abc = trackCopy("abc");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < abc.length(); i++) {
      sb.append(abc.charAt(i));
    }
    String result = sb.toString();

    assertThat(result).isEqualTo("abc");

    assertThatTracker(getStringTracker(result)).matches(snapshot().trackString(abc, 0, 3));
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

    assertThatTrackerOf(array).matches(snapshot().gap(1).trackString(abc, 1, 1));
    // if we would track secondLast through the loop, then this would be:
    //   snapshotBuilder().trackString(abc, 0, 2).assertTrackerOf(array);
  }

  // we store the origin of a value before we actually call the method,
  // so what happens if it throws an exception...
  @Test public void charAtException() {
    String abc = trackCopy("abc");

    char[] array = new char[10];

    char x = abc.charAt(1);

    try {
      x = abc.charAt(1000);
    } catch (IndexOutOfBoundsException ignore) {
    }
    array[0] = x;

    // it still has the old value
    assertThatTrackerOf(array).matches(snapshot().trackString(abc, 1, 1));
  }

  char[] chars = new char[]{FlowTester.untrackedChar('.')};

  // TODO handle MergedValue coming from InvocationArgValue; doesn't work because
  //  MergedValue.getCreationInsn is null, so MergedValue.isTrackable() is false
  @Test public void mergeWithParamSwitch() {
    doMergeWithParamSwitch(ft.createSourceChar('a'), 1);
    assertThat(FlowTester.getCharSourcePoint(chars[0])).isNull();
  }

  @SuppressWarnings("all")
  void doMergeWithParamSwitch(char c, int i) {
    switch (i) {
      case 2:
        c = 'x';
    }
    chars[0] = c;
  }

  // see mergeWithParamSwitch. for some reason if statements and switch statements end up with
  // different set of merges, which in tests made them behave a bit differently; so we're testing
  // both.
  @Test public void mergeWithParamIf() {
    doMergeWithParamIf(ft.createSourceChar('a'), false);
    assertThat(FlowTester.getCharSourcePoint(chars[0])).isNull();
  }

  @SuppressWarnings("all")
  void doMergeWithParamIf(char c, boolean b) {
    if (b) {
      c = 'x';
    }
    chars[0] = c;
  }

  @Test public void cast() {
    int i = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((byte) i);
    ft.assertIsTheTrackedValue((char) i);
    ft.assertIsTheTrackedValue((byte) (int) (byte) i);
    ft.assertIsTheTrackedValue((char) (int) (char) i);
    ft.assertIsTheTrackedValue((char) (short) (char) i);
  }

  // Test that we track a value through "x & 255". (Analyzed code may use that to convert a signed
  // byte into an unsigned value.)
  @Test public void binaryAnd() {
    byte b = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((char) (b & 255));
    ft.assertIsTheTrackedValue((char) (255 & b));
  }

  @Test public void byteToUnsignedInt() {
    byte b = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((byte) Byte.toUnsignedInt(b));
  }

  // combining some things, but redundant with other tests.
  // this test is similar to what Latin1String.inflate does
  @Test public void charToByte() {
    byte[] src = trackedByteArray("abcd");
    char[] dst = new char[3];
    for (int i = 0; i < 3; i++) {
      // test case from Latin1String.inflate
      dst[i] = (char)(src[i + 1] & 255);
    }

    assertThatTrackerOf(dst).matches(snapshot().track(src, 1, 3));
  }

  /** Test handling of a jump; mostly if frames are correctly updated by the LocalVariablesSorter */
  @SuppressWarnings("ConstantConditions")
  @Test public void jump() {
    char ch = ft.createSourceChar('a');
    if (false) {
      return;
    }
    ft.assertIsTheTrackedValue(ch);
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void jump2() {
    char ch;
    do {
      ch = ft.createSourceChar('a');
    } while (false);
    ft.assertIsTheTrackedValue(ch);
  }

  /**
   * Regression test that tests that when we get a value out of an array, we find out where it came
   * from at that point; and not at the moment use that value because by then the value in the array
   * might have already changed.
   */
  @Test public void arrayLoadMutatedBeforeUse() {
    FlowTester ft2 = new FlowTester();

    char[] array1 = new char[1];
    array1[0] = ft.createSourceChar('a');

    // testing that we track where gotA and gotB come from based on the time they were read; not
    // the time they were used
    char gotA = array1[0];
    array1[0] = ft2.createSourceChar('b');
    char gotB = array1[0];

    char[] target = new char[2];
    target[0] = gotA;
    target[1] = gotB;

    assertThatTrackerOf(target).matches(snapshot()
        .part(ft.theSource(), ft.theSourceIndex(), 1)
        .part(ft2.theSource(), ft2.theSourceIndex(), 1));
  }
}
