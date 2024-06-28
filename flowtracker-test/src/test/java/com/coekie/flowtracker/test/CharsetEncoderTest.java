package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackedCharArray;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.Growth;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * Test tracking through CharsetEncoder. There is no instrumenting specifically for that class, this
 * is handled through the general flow tracking.
 * Testing this separately anyway, because this is important for correctly tracking Strings.
 */
public class CharsetEncoderTest {
  @Test
  public void test() {
    ByteBuffer bb = ByteBuffer.allocate(5);
    char[] abc = trackedCharArray("abc");
    // this is taken care of by java.lang.StringCoding.implEncodeAsciiArray (because it is in the
    // "Handle ASCII-only prefix" part in sun.nio.cs.UTF_8$Encoder.encodeArrayLoop
    utf8Encoder().encode(CharBuffer.wrap(abc), bb, false);

    assertThat(bb.array()).isEqualTo("abc\0\0".getBytes());
    assertThatTrackerOf(bb.array()).matches(snapshot().track(3, abc, 0));
  }

  // not a real test, just gathering some good test data
  @Test
  public void examples() {
    assertThat("\u00A3".getBytes().length).isEqualTo(2); // £
    assertThat("\u0939".getBytes().length).isEqualTo(3); // ह
    assertThat("\u20AC".getBytes().length).isEqualTo(3); // €
    assertThat("\uD55C".getBytes().length).isEqualTo(3); // 한
    // surrogate pair
    assertThat("\ud83c\udf09".getBytes().length).isEqualTo(4); // 🌉
  }

  @Test
  public void testNonAscii() {
    ByteBuffer bb = ByteBuffer.allocate(6);
    char[] chars = trackedCharArray("a\u00A3bc");
    utf8Encoder().encode(CharBuffer.wrap(chars), bb, false);

    assertThat(bb.array()).isEqualTo("a\u00A3bc\0".getBytes());

    // both the second and the third byte come from the char at position 1.
    // it would have been more _natural_ if that resulted in something like this; where the two
    // bytes coming from the same char use Growth.DOUBLE:
    // snapshot().track(1, chars, 0).track(2, chars, 1, Growth.DOUBLE).track(2, chars, 2)
    // it works out a little different, but this is actually equivalent: the first two bytes come
    // from chars 0-1, and the next 3 bytes come from chars 1-4
    assertThatTrackerOf(bb.array()).matches(snapshot().track(2, chars, 0).track(3, chars, 1));

    // in its simplified form, this means we correctly tracked the full range of chars to the full
    // range of bytes
    assertThatTrackerOf(bb.array()).simplified().matches(snapshot()
        .track(5, chars, 0, Growth.of(5, 4)));
  }

  // TODO test cases with surrogate pairs (especially tricky with
  //  surrogate pair that are split between multiple encode rounds)

  private CharsetEncoder utf8Encoder() {
    // testing CodingErrorAction.REPLACE, because that's also what StreamEncoder does by default.
    // other modes are to be tested/supported later
    return StandardCharsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }
}
