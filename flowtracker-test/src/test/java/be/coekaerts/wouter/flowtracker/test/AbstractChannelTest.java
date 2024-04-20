package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
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
      snapshotBuilder().part(getReadTracker(channel), 0, 3).assertTrackerOf(bb.array());
    }
  }

  @Test
  public void readMultiple() throws IOException {
    try (C channel = openForRead()) {
      ByteBuffer bb = ByteBuffer.allocate(2);
      assertThat(channel.read(bb)).isEqualTo(2);
      assertReadContentEquals("ab", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 2).assertTrackerOf(bb.array());

      bb.position(0);
      assertThat(channel.read(bb)).isEqualTo(1);
      assertReadContentEquals("abc", channel);
      snapshotBuilder().part(getReadTracker(channel), 2, 1).part(getReadTracker(channel), 1, 1)
          .assertTrackerOf(bb.array());
    }
  }

  @Test
  public void write() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer bb = ByteBuffer.wrap(trackedByteArray("abc"));
      channel.write(bb);
      assertWrittenContentEquals("abc", channel);
      snapshotBuilder().track(bb.array()).assertEquals(getWriteTracker(channel));
    }
  }

  @Test
  public void writeMultiple() throws IOException {
    try (C channel = openForWrite()) {
      ByteBuffer bb1 = ByteBuffer.wrap(trackedByteArray("abc"));
      ByteBuffer bb2 = ByteBuffer.wrap(trackedByteArray("def"));
      channel.write(bb1);
      channel.write(bb2);
      assertWrittenContentEquals("abcdef", channel);
      snapshotBuilder().track(bb1.array()).track(bb2.array())
          .assertEquals(getWriteTracker(channel));
    }
  }

  // TODO hook read methods that take a ByteBuffer[] (GatheringByteChannel, ScatteringByteChannel)
  // TODO handle direct ByteBuffers

  void assertReadContentEquals(String expected, C channel) {
    var tracker = (ByteOriginTracker) requireNonNull(getReadTracker(channel));
    assertThat(tracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }

  void assertWrittenContentEquals(String expected, C channel) {
    var tracker = (ByteSinkTracker) requireNonNull(getWriteTracker(channel));
    assertThat(tracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
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
