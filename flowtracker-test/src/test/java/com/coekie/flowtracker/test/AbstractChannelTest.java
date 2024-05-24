package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import org.junit.Test;

abstract class AbstractChannelTest<C extends ByteChannel> {
  abstract C openForRead() throws IOException;
  abstract C openForWrite() throws IOException;
  abstract FileDescriptor getFd(C channel);

  @Test
  public void read() throws IOException {
    try (C channel = openForRead()) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      assertThat(channel.read(bb)).isEqualTo(3);
      assertReadContentEquals("abc", channel);
      TrackerSnapshot.assertThatTrackerOf(bb.array()).matches(
          TrackerSnapshot.snapshot().part(3, getReadTracker(channel), 0));
    }
  }

  // TODO test read with ByteBuffer with non-zero offset and position
  //  (like writeWithBufferOffsetAndPosition)

  @Test
  public void readMultiple() throws IOException {
    try (C channel = openForRead()) {
      ByteBuffer bb = ByteBuffer.allocate(2);
      assertThat(channel.read(bb)).isEqualTo(2);
      assertReadContentEquals("ab", channel);
      TrackerSnapshot.assertThatTrackerOf(bb.array()).matches(
          TrackerSnapshot.snapshot().part(2, getReadTracker(channel), 0));

      bb.position(0);
      assertThat(channel.read(bb)).isEqualTo(1);
      assertReadContentEquals("abc", channel);
      TrackerSnapshot.assertThatTrackerOf(bb.array()).matches(
          TrackerSnapshot.snapshot().part(1, getReadTracker(channel), 2).part(1, getReadTracker(channel), 1));
    }
  }

  @Test
  public void write() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer bb = ByteBuffer.wrap(trackedByteArray("abc"));
      channel.write(bb);
      assertWrittenContentEquals("abc", channel);
      TrackerSnapshot.assertThatTracker(getWriteTracker(channel)).matches(
          TrackerSnapshot.snapshot().track(bb.array()));
    }
  }

  /** Test writing with a ByteBuffer with non-zero ByteBuffer.offset and position */
  @Test
  public void writeWithBufferOffsetAndPosition() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer orig = ByteBuffer.wrap(trackedByteArray("abcdefg"));
      orig.position(1);
      ByteBuffer bb = orig.slice();
      bb.position(2);
      bb.limit(5);
      channel.write(bb);
      // bb has offset=1, position=2, so write should start at 1+2=3
      assertWrittenContentEquals("def", channel);
      TrackerSnapshot.assertThatTracker(getWriteTracker(channel)).matches(
          TrackerSnapshot.snapshot().track(3, orig.array(), 3));
    }
  }

  // TODO test write with read-only buffer (use ByteBufferHook.hb and offset?)

  @Test
  public void writeMultiple() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer bb1 = ByteBuffer.wrap(trackedByteArray("abc"));
      ByteBuffer bb2 = ByteBuffer.wrap(trackedByteArray("def"));
      channel.write(bb1);
      channel.write(bb2);
      assertWrittenContentEquals("abcdef", channel);
      TrackerSnapshot.assertThatTracker(getWriteTracker(channel)).matches(
          TrackerSnapshot.snapshot().track(bb1.array()).track(bb2.array()));
    }
  }

  /** Test writing using GatheringByteChannel */
  @Test
  public void writeGathering() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer bb1 = ByteBuffer.wrap(trackedByteArray("abc"));
      ByteBuffer bb2 = ByteBuffer.wrap(trackedByteArray("def"));
      ((GatheringByteChannel) channel).write(new ByteBuffer[]{bb1, bb2});
      assertWrittenContentEquals("abcdef", channel);
      TrackerSnapshot.assertThatTracker(getWriteTracker(channel)).matches(
          TrackerSnapshot.snapshot().track(bb1.array()).track(bb2.array()));
    }
  }

  /**
   * Test writing using GatheringByteChannel with a ByteBuffer with non-zero ByteBuffer.offset and
   * position
   */
  @Test
  public void writeGatheringWithBufferOffsetAndPosition() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer orig1 = ByteBuffer.wrap(trackedByteArray("abcdefghij"));
      orig1.position(1);
      ByteBuffer bb1 = orig1.slice();
      bb1.position(2);
      bb1.limit(5);

      ByteBuffer bb2 = ByteBuffer.wrap(trackedByteArray("xyz"));
      ((GatheringByteChannel) channel).write(new ByteBuffer[]{bb1, bb2});

      // bb1 has offset=1, position=2, so write should start at 1+2=3
      assertWrittenContentEquals("defxyz", channel);
      TrackerSnapshot.assertThatTracker(getWriteTracker(channel)).matches(
          TrackerSnapshot.snapshot().track(3, bb1.array(), 3).track(bb2.array()));
    }
  }

  /**
   * Test writing using GatheringByteChannel, with an offset.
   * Note that this is about an offset into the ByteBuffer[], not to be confused with an offset in
   * the ByteBuffer itself (like in {@link #writeGatheringWithBufferOffsetAndPosition}).
   */
  @Test
  public void writeGatheringOffset() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer bb1 = ByteBuffer.wrap(trackedByteArray("abc"));
      ByteBuffer bb2 = ByteBuffer.wrap(trackedByteArray("def"));
      ByteBuffer bb3 = ByteBuffer.wrap(trackedByteArray("ghi"));
      ((GatheringByteChannel) channel).write(new ByteBuffer[]{bb1, bb2, bb3}, 1, 2);
      assertWrittenContentEquals("defghi", channel);
      TrackerSnapshot.assertThatTracker(getWriteTracker(channel)).matches(
          TrackerSnapshot.snapshot().track(bb2.array()).track(bb3.array()));
    }
  }

  // TODO hook read methods that take a ByteBuffer[] (ScatteringByteChannel)
  // TODO handle direct ByteBuffers

  void assertReadContentEquals(String expected, C channel) {
    var tracker = (ByteOriginTracker) requireNonNull(getReadTracker(channel));
    assertThat(tracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }

  void assertWrittenContentEquals(String expected, C channel) {
    var tracker = (ByteSinkTracker) requireNonNull(getWriteTracker(channel));
    assertThat(toArray(tracker.getByteContent())).isEqualTo(expected.getBytes());
  }

  private byte[] toArray(ByteBuffer buf) {
    byte[] result = new byte[buf.remaining()];
    int pos = buf.position();
    buf.get(result);
    buf.position(pos); // restore buffer position
    return result;
  }

  ByteOriginTracker getReadTracker(C channel) {
    return FileDescriptorTrackerRepository.getReadTracker(getFd(channel));
  }

  ByteSinkTracker getWriteTracker(C channel) {
    return FileDescriptorTrackerRepository.getWriteTracker(getFd(channel));
  }

  /** Content that tests for reading expect */
  static byte[] contentForReading() {
    return new byte[]{'a', 'b', 'c'};
  }
}
