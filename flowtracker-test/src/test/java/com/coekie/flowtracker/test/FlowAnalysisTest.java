package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.FlowTester.getCharSourcePoint;
import static com.coekie.flowtracker.test.FlowTester.untrackedChar;
import static com.coekie.flowtracker.test.TrackTestHelper.assertIsFallbackIn;
import static com.coekie.flowtracker.test.TrackTestHelper.trackCopy;
import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;

import com.coekie.flowtracker.tracker.TrackerSnapshot;
import org.junit.Test;

/**
 * Test FlowTransformer, FlowInterpreter and friends. This is a mix of mostly tests that don't have
 * a good home in another test class, either because they're not worth splitting out (e.g. tests for
 * how some particular ops are handled in the interpreter), or testing a generic mechanism (not for
 * a specific type of FlowValue or Store).
 */
public class FlowAnalysisTest {
  private final FlowTester ft = new FlowTester();

  @Test public void fallback() {
    char[] array = new char[2];
    array[0] = untrackedChar('c');
    assertIsFallbackIn(getCharSourcePoint(array[0]), FlowAnalysisTest.class.getName());
  }

  // Tricky flow in a loop.
  // Both assignments into array come from the same statement,
  // but "secondLast" does not contain the *last* execution of that statement anymore.
  @Test public void delayedFlow() {
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

   TrackerSnapshot.assertThatTrackerOf(array).matches(TrackerSnapshot.snapshot()
      .trackString(2, abc, 0));
  }

  // Test that we track a value through "x & 255". (Analyzed code may use that to convert a signed
  // byte into an unsigned value.)
  @Test public void binaryAnd() {
    byte b = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((char) (b & 255));
    ft.assertIsTheTrackedValue((char) (255 & b));
  }

  @Test public void binaryAndLong() {
    long i = ft.createSourceLong('a');
    ft.assertIsTheTrackedValue((char) (i & 255));
    ft.assertIsTheTrackedValue((char) (255 & i));
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
    ft.assertIsTheTrackedValue((byte) ((i >> 8) & 0xFF));
  }

  @Test public void shiftLong() {
    long i = ft.createSourceLong(0x6162);
    ft.assertIsTheTrackedValue((byte) (i >>> 8));
    ft.assertIsTheTrackedValue((byte) ((i >>> 8) & 0xFF));
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
}
