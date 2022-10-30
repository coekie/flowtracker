package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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

    assertArrayEquals("abc\0\0".getBytes(), bb.array());
    snapshotBuilder().track(abc, 0, 3).assertTrackerOf(bb.array());
  }

  // not a real test, just gathering some good test data
  @Test
  public void examples() {
    assertEquals(2, "\u00A3".getBytes().length); // $
    assertEquals(3, "\u0939".getBytes().length); // ह
    assertEquals(3, "\u20AC".getBytes().length); // €
    assertEquals(3, "\uD55C".getBytes().length); // 한
    // surrogate pair
    assertEquals(4, "\ud83c\udf09".getBytes().length); // 🌉
  }

  @Test
  public void testNonAscii() {
    ByteBuffer bb = ByteBuffer.allocate(6);
    char[] chars = trackedCharArray("a\u00A3bc");
    utf8Encoder().encode(CharBuffer.wrap(chars), bb, false);

    assertArrayEquals("a\u00A3bc\0".getBytes(), bb.array());
    // TODO this isn't right, lost track at the non-ascii char
    snapshotBuilder().track(chars, 0, 1).assertTrackerOf(bb.array());
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
