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

public class FileInputStreamHookTest {
  private static File file;

  @BeforeClass public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_InputStreamHookTest", "txt").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{1, 2, 3});
    }
  }

  @AfterClass public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  @Test public void descriptor() throws IOException {
    var fisFromFile = new FileInputStream(file);
    TrackTestHelper.assertDescriptor(fisFromFile, "FileInputStream for " + file.getPath(), null);

    var fisFromName = new FileInputStream(file.getPath());
    TrackTestHelper.assertDescriptor(fisFromName, "FileInputStream for " + file.getPath(), null);
  }

  @Test public void read() throws IOException {
    try (var fis = new FileInputStream(file)) {
      var fisTracker = (ByteOriginTracker) requireNonNull(TrackerRepository.getTracker(fis));

      byte[] buffer = new byte[5];
      assertEquals(3, fis.read(buffer));

      snapshotBuilder().part(fisTracker, 0, 3)
          .assertTrackerOf(buffer);

      assertEquals(ByteBuffer.wrap(new byte[]{1, 2, 3}), fisTracker.getByteContent());
    }
  }

  @Test public void readWithOffset() throws IOException {
    try (var fis = new FileInputStream(file)) {
      var fisTracker = (ByteOriginTracker) requireNonNull(TrackerRepository.getTracker(fis));

      byte[] buffer = new byte[5];
      assertEquals(3, fis.read(buffer, 1, 4));

      snapshotBuilder().gap(1).part(fisTracker, 0, 3)
          .assertTrackerOf(buffer);

      assertEquals(ByteBuffer.wrap(new byte[]{1, 2, 3}), fisTracker.getByteContent());
    }
  }

  @Test public void readSingleByte() throws IOException {
    try (var fis = new FileInputStream(file)) {
      var fisTracker = (ByteOriginTracker) requireNonNull(TrackerRepository.getTracker(fis));

      assertEquals(1, fis.read());

      assertEquals(ByteBuffer.wrap(new byte[]{1}), fisTracker.getByteContent());
    }
  }
}
