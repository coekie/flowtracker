package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.Reflection;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileChannelTest extends AbstractChannelTest<FileChannel> {
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
    boolean deleted = fileToRead.delete();
    assertThat(deleted).isTrue();
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
  public void node() throws IOException {
    try (FileChannel channel = openForRead()) {
      TrackTestHelper.assertThatTrackerNode(getReadTracker(channel))
          .hasPathStartingWith("Files")
          .hasPathEndingWith(fileToRead.getName());
    }
  }

  @Test
  public void nodeWithReadAndWrite() throws IOException {
    try (FileChannel channel = FileChannel.open(fileToWrite.toPath(),
        StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      TrackTestHelper.assertThatTrackerNode(getReadTracker(channel))
          .hasPathStartingWith("Files")
          .hasPathEndingWith(fileToWrite.getName(), FileDescriptorTrackerRepository.READ);
      TrackTestHelper.assertThatTrackerNode(getWriteTracker(channel))
          .hasPathStartingWith("Files")
          .hasPathEndingWith(fileToWrite.getName(), FileDescriptorTrackerRepository.WRITE);
    }
  }

  @Test
  public void readWithPosition() throws IOException {
    try (FileChannel channel = openForRead()) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      channel.read(bb, 1);
      assertReadContentEquals("bc", channel);
      TrackerSnapshot.assertThatTrackerOf(bb.array()).matches(
          TrackerSnapshot.snapshot().part(2, getReadTracker(channel), 0));
    }
  }

  // TODO track seeking (using channel.position & passing position argument to operations)
  // TODO FileChannel.transferFrom / transferTo

  @Override
  FileDescriptor getFd(FileChannel channel) {
    return Reflection.getSlow(Reflection.clazz("sun.nio.ch.FileChannelImpl"), "fd",
        FileDescriptor.class, channel);
  }
}
