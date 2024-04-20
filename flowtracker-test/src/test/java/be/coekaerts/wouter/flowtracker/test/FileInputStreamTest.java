package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test instrumentation of {@link FileInputStream} */
public class FileInputStreamTest extends AbstractInputStreamTest {
  private static File file;

  @BeforeClass public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_InputStreamHookTest", "").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
  }

  @AfterClass public static void removeTmpFile() {
    boolean deleted = file.delete();
    assertThat(deleted).isTrue();
  }

  @Override
  FileInputStream createInputStream(byte[] bytes) throws IOException {
    try (var out = new FileOutputStream(file)) {
      out.write(bytes);
    }
    return new FileInputStream(file);
  }

  @Override
  Tracker getStreamTracker(InputStream is) {
    try {
      return FileDescriptorTrackerRepository.getReadTracker(((FileInputStream) is).getFD());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Test public void node() throws IOException {
    try (var fisFromFile = new FileInputStream(file)) {
      TrackTestHelper.assertThatTracker(getStreamTracker(fisFromFile))
          .hasNodeStartingWith("Files")
          .hasNodeEndingWith(file.getName());
    }

    try (var fisFromName = new FileInputStream(file.getPath())) {
      TrackTestHelper.assertThatTracker(getStreamTracker(fisFromName))
          .hasNodeStartingWith("Files")
          .hasNodeEndingWith(file.getName());
    }
  }

  @Test
  public void channel() throws IOException {
    try (FileInputStream is = createInputStream("1234".getBytes())) {
      // first read from the FileInputStream
      byte[] buffer = new byte[2];
      assertThat(is.read(buffer)).isEqualTo(2);

      // then read from the associated FileChannel
      ByteBuffer bb = ByteBuffer.wrap(new byte[2]);
      assertThat(is.getChannel().read(bb)).isEqualTo(2);
      snapshotBuilder().part(getStreamTracker(is), 2, 2).assertTrackerOf(bb.array());

      assertContentEquals("1234", is);
    }
  }
}
