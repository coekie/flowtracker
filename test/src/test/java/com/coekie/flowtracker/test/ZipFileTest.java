package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import com.google.common.truth.Truth;
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
        Tracker tracker = TrackerRepository.getTracker(context(), in);
        TrackerSnapshot.assertThatTrackerOf(bytes).matches(
            TrackerSnapshot.snapshot().part(bytes.length, tracker, 0));
        Truth.assertThat(((ByteOriginTracker) tracker).getByteContent())
            .isEqualTo(ByteBuffer.wrap(bytes));
      }
    }
  }
}
