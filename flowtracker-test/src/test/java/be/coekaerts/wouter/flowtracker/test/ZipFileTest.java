package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Before;
import org.junit.Test;

public class ZipFileTest {
  private String testZipFilePath;
  private static final String testZipFileEntryName = "org/junit/Test.class";

  @Before
  public void before() throws IOException {
    URL jarURL = requireNonNull(Test.class.getResource("Test.class"));
    URL fileURL = new URL(jarURL.getPath());
    testZipFilePath = fileURL.getPath().substring(0, fileURL.getPath().indexOf('!'));
  }

  @Test
  public void test() throws IOException {
    try (ZipFile zipFile = new ZipFile(testZipFilePath)) {
      ZipEntry entry = zipFile.getEntry(testZipFileEntryName);
      try (InputStream in = zipFile.getInputStream(entry)) {
        TrackTestHelper.assertThatTrackerNode(in)
            .hasPathStartingWith("Files")
            .hasPathMatching(nodePath -> nodePath.contains(new File(testZipFilePath).getName()))
            .hasPathEndingWith("junit", "Test.class");

        // this is tested more in InflaterInputStreamTest
        byte[] bytes = in.readAllBytes();
        Tracker tracker = TrackerRepository.getTracker(in);
        assertThatTrackerOf(bytes).matches(snapshot().part(tracker, 0, bytes.length));
        assertThat(((ByteOriginTracker) tracker).getByteContent())
            .isEqualTo(ByteBuffer.wrap(bytes));
      }
    }
  }
}
