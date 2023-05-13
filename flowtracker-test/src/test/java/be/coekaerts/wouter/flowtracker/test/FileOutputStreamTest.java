package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
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
    assertTrue(file.delete());
  }

  @Test public void descriptor() throws IOException {
    try (var fosFromFile = new FileOutputStream(file)) {
      TrackTestHelper.assertDescriptor(getTracker(fosFromFile),
          "FileOutputStream for " + file.getPath(), null);
    }

    try (var fosFromName = new FileOutputStream(file.getPath())) {
      TrackTestHelper.assertDescriptor(getTracker(fosFromName),
          "FileOutputStream for " + file.getPath(), null);
    }
  }

  @Override
  @Test
  // TODO FileOutputStreamTest.writeSingleByte no tracking of single char write
  public void writeSingleByte() throws IOException {
    FlowTester flowTester0 = new FlowTester();
    FlowTester flowTester1 = new FlowTester();
    try (var os = createOutputStream()) {
      os.write(flowTester0.createSourceChar('a'));
      os.write(flowTester1.createSourceChar('b'));

      assertContentEquals("ab", os);
      snapshotBuilder()
          .gap(2)
          .assertEquals(getTracker(os));
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
      snapshotBuilder().track(abc, 0, 3).track(bb.array(), 0, 3).assertEquals(getTracker(os));
    }
  }

  @Override
  FileOutputStream createOutputStream() throws IOException {
    return new FileOutputStream(file);
  }

  @Override
  Tracker getTracker(FileOutputStream os) {
    try {
      return FileDescriptorTrackerRepository.getWriteTracker(os.getFD());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  void assertContentEquals(String expected, FileOutputStream os) {
    var tracker = (ByteSinkTracker) requireNonNull(getTracker(os));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }
}
