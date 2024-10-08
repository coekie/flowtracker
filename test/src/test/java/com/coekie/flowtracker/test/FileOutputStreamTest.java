package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test instrumentation of {@link FileOutputStream} */
public class FileOutputStreamTest extends AbstractOutputStreamTest<FileOutputStream> {
  private static File file;

  @BeforeClass
  public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_FileOutputStreamTest", "").toFile();
  }

  @AfterClass
  public static void removeTmpFile() {
    boolean deleted = file.delete();
    assertThat(deleted).isTrue();
  }

  @Test public void node() throws IOException {
    try (var fosFromFile = new FileOutputStream(file)) {
      TrackTestHelper.assertThatTrackerNode(getTracker(fosFromFile))
          .hasPathStartingWith("Files")
          .hasPathEndingWith(file.getName());
    }

    try (var fosFromName = new FileOutputStream(file.getPath())) {
      TrackTestHelper.assertThatTrackerNode(getTracker(fosFromName))
          .hasPathStartingWith("Files")
          .hasPathEndingWith(file.getName());
    }
  }

  @Test
  public void channel() throws IOException {
    byte[] abc = trackedByteArray("abc");
    ByteBuffer bb = ByteBuffer.wrap(trackedByteArray("def"));
    try (var os = createOutputStream()) {
      // first write to FileOutputStream
      os.write(abc);

      // then write through associated FileChannel
      os.getChannel().write(bb);

      assertContentEquals("abcdef", os);
      TrackerSnapshot.assertThatTracker(getTracker(os)).matches(
          TrackerSnapshot.snapshot().track(3, abc, 0).track(3, bb.array(), 0));
    }
  }

  @Override
  FileOutputStream createOutputStream() throws IOException {
    return new FileOutputStream(file);
  }

  @Override
  Tracker getTracker(FileOutputStream os) {
    try {
      return FileDescriptorTrackerRepository.getWriteTracker(context(), os.getFD());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  void assertContentEquals(String expected, FileOutputStream os) {
    var tracker = (ByteSinkTracker) requireNonNull(getTracker(os));
    assertThat(tracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }
}
