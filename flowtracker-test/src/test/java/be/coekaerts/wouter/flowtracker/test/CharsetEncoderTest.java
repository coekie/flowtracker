package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

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
    assertThat("\u00A3".getBytes().length).isEqualTo(2); // $
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
    // TODO this isn't right, lost track of the non-ascii char
    assertThatTrackerOf(bb.array()).matches(snapshot()
        .track(1, chars, 0).gap(2).track(2, chars, 2));
  }

  // TODO test non-ASCII chars. also test cases with surrogate pairs (especially tricky with
  //  surrogate pair that are split between multiple encode rounds)

  private CharsetEncoder utf8Encoder() {
    // testing CodingErrorAction.REPLACE, because that's also what StreamEncoder does by default.
    // other modes are to be tested/supported later
    return StandardCharsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }
}
