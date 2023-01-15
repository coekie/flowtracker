package be.coekaerts.wouter.flowtracker.test;

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
