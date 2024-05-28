package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackCopy;
import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.StringHook;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import org.junit.Test;

/** Test FlowTransformer and friends */
@SuppressWarnings("StringBufferMayBeStringBuilder")
public class FlowAnalysisTest {
  private final FlowTester ft = new FlowTester();

  @Test public void stringBuilderAppendChar() {
    String abc = trackCopy("abc");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < abc.length(); i++) {
      sb.append(abc.charAt(i));
    }
    String result = sb.toString();

    assertThat(result).isEqualTo("abc");

    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot().trackString(3, abc, 0));
  }

  @Test public void stringBufferAppendChar() {
    String abc = trackCopy("abc");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < abc.length(); i++) {
      sb.append(abc.charAt(i));
    }
    String result = sb.toString();

    assertThat(result).isEqualTo("abc");

    TrackerSnapshot.assertThatTracker(StringHook.getStringTracker(result)).matches(
        TrackerSnapshot.snapshot().trackString(3, abc, 0));
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

    TrackerSnapshot.assertThatTrackerOf(array).matches(
        TrackerSnapshot.snapshot().gap(1).trackString(1, abc, 1));
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
    TrackerSnapshot.assertThatTrackerOf(array).matches(
        TrackerSnapshot.snapshot().trackString(1, abc, 1));
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

  // Test that we track a value through ">>>" (IUSHR).
  @SuppressWarnings("PointlessBitwiseExpression")
  @Test public void shift() {
    int i = ft.createSourceInt(0x6162);
    // see e.g. DataOutputStream.writeShort/writeChar, Bits.putShort/putInt
    ft.assertIsTheTrackedValue((byte) (i >>> 8));
    ft.assertIsTheTrackedValue((byte) (i >>> 0));
    ft.assertIsTheTrackedValue((byte) ((i >>> 8) & 0xFF));
    ft.assertIsTheTrackedValue((byte) ((i >>> 0) & 0xFF));
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

    TrackerSnapshot.assertThatTrackerOf(dst).matches(TrackerSnapshot.snapshot().track(3, src, 1));
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

    TrackerSnapshot.assertThatTrackerOf(target).matches(TrackerSnapshot.snapshot()
        .part(ft.point())
        .part(ft2.point()));
  }
}
