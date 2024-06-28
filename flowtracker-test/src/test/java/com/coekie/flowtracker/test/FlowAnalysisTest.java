package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.FlowTester.getCharSourcePoint;
import static com.coekie.flowtracker.test.FlowTester.untrackedChar;
import static com.coekie.flowtracker.test.TrackTestHelper.assertIsFallbackIn;
import static com.coekie.flowtracker.test.TrackTestHelper.trackCopy;
import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.Growth;
import com.coekie.flowtracker.tracker.TrackerRepository;
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

   assertThatTrackerOf(array).matches(TrackerSnapshot.snapshot()
      .trackString(2, abc, 0));
  }

  // Test that we track a value through "x & 255". (Analyzed code may use that to convert a signed
  // byte into an unsigned value.)
  @Test public void binaryAndConstant() {
    byte b = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((char) (b & 255));
    ft.assertIsTheTrackedValue((char) (255 & b));
  }

  @Test public void binaryAndConstantLong() {
    long i = ft.createSourceLong('a');
    ft.assertIsTheTrackedValue((char) (i & 255));
    ft.assertIsTheTrackedValue((char) (255 & i));
  }

  @Test public void binaryOrConstant() {
    // not sure if this matters. the value is a combination of two, so we pick the non-constant one
    // as the one that's presumably the most interesting one. For consistency with '&'
    byte b = ft.createSourceByte((byte) 'a');
    ft.assertIsTheTrackedValue((char) (b | 128));
    ft.assertIsTheTrackedValue((char) (128 | b));
  }

  @Test public void binaryOrConstantLong() {
    long i = ft.createSourceLong('a');
    ft.assertIsTheTrackedValue((char) (i | 128));
    ft.assertIsTheTrackedValue((char) (128 | i));
  }

  @Test public void binaryOrCombine() {
    byte[] bytes = trackedByteArray("abcdef");
    char[] chars = new char[2];
    chars[0] = (char) ((bytes[0] << 8) | bytes[1]);
    chars[1] = (char) (bytes[3] | (bytes[2] << 8)); // other order
    assertThatTrackerOf(chars).matches(snapshot()
        .part(2, TrackerRepository.getTracker(context(), bytes), 0, Growth.HALF));

    // test handling of points that already have length > 1:
    // combining two points with length two gives one of length 4
    int[] ints = new int[1];
    ints[0] = (chars[0] << 16) | chars[1];
    assertThatTrackerOf(ints).matches(snapshot()
        .part(1, TrackerRepository.getTracker(context(), bytes), 0, Growth.of(1, 4)));
  }

  @Test public void binaryOrCombineMismatch() {
    byte[] bytes1 = trackedByteArray("abc");
    byte[] bytes2 = trackedByteArray("xyz");
    char[] chars = new char[2];
    chars[0] = (char) ((bytes1[0] << 8) | bytes2[1]);
    assertThat(getCharSourcePoint(chars[0])).isNull();
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

    assertThatTrackerOf(dst).matches(TrackerSnapshot.snapshot().track(3, src, 1));
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
