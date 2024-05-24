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

  // TODO test write with buffer with non-zero position and offset (slice)

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
