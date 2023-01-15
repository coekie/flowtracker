package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.Reflection;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileChannelTest {
  private static final Field fdField = fdField();

  private static File fileToRead;
  private static File fileToWrite;

  @BeforeClass
  public static void createTmpFile() throws IOException {
    fileToRead = Files.createTempFile("flowtracker_FileChannelTest_read", "").toFile();
    try (var out = new FileOutputStream(fileToRead)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
    fileToWrite = Files.createTempFile("flowtracker_FileChannelTest_write", "").toFile();
  }

  @AfterClass
  public static void removeTmpFile() {
    assertTrue(fileToRead.delete());
  }

  @Test
  public void descriptor() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToRead.toPath(), StandardOpenOption.READ)) {
      TrackTestHelper.assertInterestAndDescriptor(
          getReadTracker(channel),
          "FileChannel for " + fileToRead.getPath(), null);
    }
  }

  @Test
  public void read() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToRead.toPath(), StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      assertEquals(3, channel.read(bb));
      assertReadContentEquals("abc", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 3).assertTrackerOf(bb.array());
    }
  }

  @Test
  public void readMultiple() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToRead.toPath(), StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(2);
      assertEquals(2, channel.read(bb));
      assertReadContentEquals("ab", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 2).assertTrackerOf(bb.array());

      bb.position(0);
      assertEquals(1, channel.read(bb));
      assertReadContentEquals("abc", channel);
      snapshotBuilder().part(getReadTracker(channel), 2, 1).part(getReadTracker(channel), 1, 1)
          .assertTrackerOf(bb.array());
    }
  }

  @Test
  public void readWithPosition() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToRead.toPath(), StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      channel.read(bb, 1);
      assertReadContentEquals("bc", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 2).assertTrackerOf(bb.array());
    }
  }

  // TODO hook FileChannel.read methods that take a ByteBuffer[]
  // TODO handle direct ByteBuffers
  // TODO track seeking (using channel.position & passing position argument to operations)
  // TODO Channel.transferFrom / transferTo

  @Test
  public void write() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToWrite.toPath(), StandardOpenOption.WRITE)) {
      ByteBuffer bb = ByteBuffer.wrap(trackedByteArray("abc"));
      channel.write(bb);
      assertWrittenContentEquals("abc", channel);
      snapshotBuilder().track(bb.array()).assertEquals(getWriteTracker(channel));
    }
  }

  @Test
  public void writeMultiple() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToWrite.toPath(), StandardOpenOption.WRITE)) {
      ByteBuffer bb1 = ByteBuffer.wrap(trackedByteArray("abc"));
      ByteBuffer bb2 = ByteBuffer.wrap(trackedByteArray("def"));
      channel.write(bb1);
      channel.write(bb2);
      assertWrittenContentEquals("abcdef", channel);
      snapshotBuilder().track(bb1.array()).track(bb2.array())
          .assertEquals(getWriteTracker(channel));
    }
  }

  private void assertReadContentEquals(String expected, FileChannel channel) {
    var tracker = (ByteOriginTracker) requireNonNull(getReadTracker(channel));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }

  private void assertWrittenContentEquals(String expected, FileChannel channel) {
    var tracker = (ByteSinkTracker) requireNonNull(getWriteTracker(channel));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }

  static ByteOriginTracker getReadTracker(FileChannel channel) {
    return FileDescriptorTrackerRepository.getReadTracker(getFd(channel));
  }

  static ByteSinkTracker getWriteTracker(FileChannel channel) {
    return FileDescriptorTrackerRepository.getWriteTracker(getFd(channel));
  }

  static FileDescriptor getFd(FileChannel channel) {
    return (FileDescriptor) Reflection.getFieldValue(channel, fdField);
  }

  private static Field fdField() {
    try {
      Class<?> clazz = Class.forName("sun.nio.ch.FileChannelImpl");
      return Reflection.getDeclaredField(clazz, "fd");
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    }
  }
}
