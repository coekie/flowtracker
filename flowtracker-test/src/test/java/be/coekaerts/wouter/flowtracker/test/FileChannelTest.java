package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.Reflection;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ChannelTrackerRepository;
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

  private static File file;

  @BeforeClass
  public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_FileChannelTest", "").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
  }

  @AfterClass
  public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  @Test
  public void descriptor() throws IOException {
    try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
      TrackTestHelper.assertInterestAndDescriptor(
          getReadTracker(channel),
          "Read channel for " + file.getPath(), null);
    }
  }

  @Test
  public void read() throws IOException {
    try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      channel.read(bb);
      assertReadContentEquals("abc", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 3).assertTrackerOf(bb.array());
    }
  }

  @Test
  public void readMultiple() throws IOException {
    try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(2);
      channel.read(bb);
      assertReadContentEquals("ab", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 2).assertTrackerOf(bb.array());

      bb.position(0);
      channel.read(bb);
      assertReadContentEquals("abc", channel);
      snapshotBuilder().part(getReadTracker(channel), 2, 1).part(getReadTracker(channel), 1, 1)
          .assertTrackerOf(bb.array());
    }
  }

  // TODO other FileChannel.read methods
  // TODO direct ByteBuffer
  // TODO FileChannel.write

  private void assertReadContentEquals(String expected, FileChannel channel) {
    var tracker = (ByteOriginTracker) requireNonNull(getReadTracker(channel));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }

  static ByteOriginTracker getReadTracker(FileChannel channel) {
    return ChannelTrackerRepository.getReadTracker(getFd(channel));
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
