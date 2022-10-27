package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test instrumentation of {@link FileInputStream} */
public class FileInputStreamTest {
  private static File file;

  @BeforeClass public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_InputStreamHookTest", "").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
  }

  @AfterClass public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  @Test public void descriptor() throws IOException {
    try (var fisFromFile = new FileInputStream(file)) {
      TrackTestHelper.assertDescriptor(fisFromFile, "FileInputStream for " + file.getPath(), null);
    }

    try (var fisFromName = new FileInputStream(file.getPath())) {
      TrackTestHelper.assertDescriptor(fisFromName, "FileInputStream for " + file.getPath(), null);
    }
  }

  @Test public void read() throws IOException {
    try (var fis = new FileInputStream(file)) {
      byte[] buffer = new byte[5];
      assertEquals(3, fis.read(buffer));

      assertContentEquals("abc", fis);
      snapshotBuilder().track(fis, 0, 3).assertTrackerOf(buffer);
    }
  }

  @Test public void readWithOffset() throws IOException {
    try (var fis = new FileInputStream(file)) {
      byte[] buffer = new byte[5];
      assertEquals(3, fis.read(buffer, 1, 4));

      assertContentEquals("abc", fis);
      snapshotBuilder().gap(1).track(fis, 0, 3)
          .assertTrackerOf(buffer);
    }
  }

  @Test public void readSingleByte() throws IOException {
    try (var fis = new FileInputStream(file)) {
      assertEquals('a', fis.read());
      assertContentEquals("a", fis);
    }
  }

  private void assertContentEquals(String expected, FileInputStream fis) {
    var tracker = (ByteOriginTracker) requireNonNull(TrackerRepository.getTracker(fis));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }
}
