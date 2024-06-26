package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.hook.DeflaterHook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import org.junit.After;
import org.junit.Test;

public class DeflaterTest {
  private final byte[] abc = TrackTestHelper.trackedByteArray("abc");
  private final byte[] xdefx = TrackTestHelper.trackedByteArray("xxxdefxxxx");

  private Deflater deflater;

  @After
  public void after() {
    if (deflater != null) {
      deflater.end();
    }
  }

  @Test
  public void testArrayToArray() {
    testDeflateToArray(InputType.ARRAY, OutputType.ARRAY);
  }

  @Test
  public void testBufferToArray() {
    testDeflateToArray(InputType.BYTE_BUFFER, OutputType.ARRAY);
  }

  @Test
  public void testArrayToBuffer() {
    testDeflateToArray(InputType.ARRAY, OutputType.BYTE_BUFFER);
  }

  @Test
  public void testBufferToBufffer() {
    testDeflateToArray(InputType.BYTE_BUFFER, OutputType.BYTE_BUFFER);
  }

  @Test public void testReset() {
    deflater = new Deflater();
    byte[] out = new byte[64];
    deflater.setInput(abc);
    deflater.deflate(out, 0, 64, Deflater.FULL_FLUSH);
    assertThat(DeflaterHook.getSinkTracker(deflater)).isNotNull();
    assertThat(DeflaterHook.getOriginTracker(deflater)).isNotNull();
    deflater.reset();
    assertThat(DeflaterHook.getSinkTracker(deflater)).isNull();
    assertThat(DeflaterHook.getOriginTracker(deflater)).isNull();
  }

  @Test public void testTwins() {
    deflater = new Deflater();
    byte[] out = new byte[64];
    deflater.setInput(abc);
    deflater.deflate(out, 0, 64, Deflater.FULL_FLUSH);
    ByteSinkTracker sinkTracker = requireNonNull(DeflaterHook.getSinkTracker(deflater));
    ByteOriginTracker originTracker = requireNonNull(DeflaterHook.getOriginTracker(deflater));
    assertThat(sinkTracker.twin).isEqualTo(originTracker);
    assertThat(originTracker.twin).isEqualTo(sinkTracker);
  }

  private void testDeflateToArray(InputType inputType, OutputType outputType) {
    deflater = new Deflater();
    byte[] out = new byte[64];

    // set `abc` as input for first deflate call
    if (inputType == InputType.ARRAY) {
      deflater.setInput(abc);
    } else {
      deflater.setInput(ByteBuffer.wrap(abc));
    }
    int result = callDeflate(outputType, deflater, out, 0, 64);
    assertThat(result).isGreaterThan(0);

    // set `def` as input for second deflate call
    if (inputType == InputType.ARRAY) {
      deflater.setInput(xdefx, 3, 3);
    } else {
      ByteBuffer buf = ByteBuffer.wrap(xdefx);
      buf.position(1);
      buf = buf.slice(); // test with buffer with non-zero offset
      buf.position(2);
      buf.limit(5);
      deflater.setInput(buf);
    }
    deflater.finish();
    result += callDeflate(outputType, deflater, out, result, 64 - result);

    assertThat(deflater.finished()).isTrue();

    ByteSinkTracker sinkTracker = DeflaterHook.getSinkTracker(deflater);
    assertThatTracker(sinkTracker).matches(snapshot().track(abc).track(3, xdefx, 3));

    ByteOriginTracker originTracker = DeflaterHook.getOriginTracker(deflater);
    assertThatTrackerOf(out).matches(snapshot().part(result, originTracker, 0));
  }

  private int callDeflate(OutputType outputType, Deflater deflater, byte[] output, int outputPos,
      int outputLen) {
    if (outputType == OutputType.ARRAY) {
      return deflater.deflate(output, outputPos, outputLen, Deflater.FULL_FLUSH);
    } else if (outputType == OutputType.BYTE_BUFFER) {
      ByteBuffer buf = ByteBuffer.wrap(output);
      buf.position(outputPos);
      buf.limit(outputPos + outputLen);
      deflater.deflate(buf, Deflater.FULL_FLUSH);
      return buf.position() - outputPos;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  enum InputType {
    ARRAY,
    BYTE_BUFFER
  }

  enum OutputType {
    ARRAY,
    BYTE_BUFFER
  }
}
