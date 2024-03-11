package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.Reflection;
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

public class FileChannelTest extends AbstractChannelTest<FileChannel> {
  private static final Field fdField = fdField();

  private static File fileToRead;
  private static File fileToWrite;

  @BeforeClass
  public static void createTmpFile() throws IOException {
    fileToRead = Files.createTempFile("flowtracker_FileChannelTest_read", "").toFile();
    try (var out = new FileOutputStream(fileToRead)) {
      out.write(contentForReading());
    }
    fileToWrite = Files.createTempFile("flowtracker_FileChannelTest_write", "").toFile();
  }

  @AfterClass
  public static void removeTmpFile() {
    assertTrue(fileToRead.delete());
  }

  @Override
  FileChannel openForRead() throws IOException {
    return FileChannel.open(fileToRead.toPath(), StandardOpenOption.READ);
  }

  @Override
  FileChannel openForWrite() throws IOException {
    return FileChannel.open(fileToWrite.toPath(), StandardOpenOption.WRITE);
  }

  @Test
  public void descriptor() throws IOException {
    try (FileChannel channel = openForRead()) {
      TrackTestHelper.assertThatTracker(getReadTracker(channel))
          .hasDescriptor("FileChannel for " + fileToRead.getPath())
          .hasNodeStartingWith("Files")
          .hasNodeEndingWith(fileToRead.getName(), FileDescriptorTrackerRepository.READ);
    }
  }

  @Test
  public void readWithPosition() throws IOException {
    try (FileChannel channel = openForRead()) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      channel.read(bb, 1);
      assertReadContentEquals("bc", channel);
      snapshotBuilder().part(getReadTracker(channel), 0, 2).assertTrackerOf(bb.array());
    }
  }

  // TODO track seeking (using channel.position & passing position argument to operations)
  // TODO FileChannel.transferFrom / transferTo

  @Override
  FileDescriptor getFd(FileChannel channel) {
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
