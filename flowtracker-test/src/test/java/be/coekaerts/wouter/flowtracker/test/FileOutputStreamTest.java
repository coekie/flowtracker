package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test instrumentation of {@link FileOutputStream} */
public class FileOutputStreamTest {
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
      TrackTestHelper.assertDescriptor(fosFromFile, "FileOutputStream for " + file.getPath(), null);
    }

    try (var fosFromName = new FileOutputStream(file.getPath())) {
      TrackTestHelper.assertDescriptor(fosFromName, "FileOutputStream for " + file.getPath(), null);
    }
  }

  @Test public void writeSingleByte() throws IOException {
    try (var fos = new FileOutputStream(file)) {
      fos.write('a');
      fos.write('b');

      // tracking source for write(int) not implemented
      assertContentEquals("ab", fos);
      snapshotBuilder().gap(2).assertTrackerOf(fos);
    }
  }

  @Test public void writeByteArray() throws IOException {
    byte[] abc = trackedByteArray("abc");
    try (var fos = new FileOutputStream(file)) {
      fos.write(abc);
      fos.write(abc);

      assertContentEquals("abcabc", fos);
      snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertTrackerOf(fos);
    }
  }

  @Test public void writeUntrackedByteArray() throws IOException {
    byte[] abc = untrackedByteArray("abc"); // without tracking
    byte[] def = trackedByteArray("def"); // with tracking to test if offset is still correct
    try (var fos = new FileOutputStream(file)) {
      fos.write(abc);
      fos.write(def);

      assertContentEquals("abcdef", fos);
      snapshotBuilder().gap(3).track(def, 0, 3).assertTrackerOf(fos);
    }
  }

  @Test public void writeByteArrayOffset() throws IOException {
    byte[] abcd = trackedByteArray("abcd");
    try (var fos = new FileOutputStream(file)) {
      fos.write(abcd, 1, 2);

      assertContentEquals("bc", fos);
      snapshotBuilder().track(abcd, 1, 2).assertTrackerOf(fos);
    }
  }

  private void assertContentEquals(String expected, FileOutputStream fos) {
    var tracker = (ByteSinkTracker) requireNonNull(TrackerRepository.getTracker(fos));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }
}
