package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    ZipFile zipFile = new ZipFile(testZipFilePath);
    ZipEntry entry = zipFile.getEntry(testZipFileEntryName);
    try (InputStream in = zipFile.getInputStream(entry)) {
      Tracker tracker = requireNonNull(TrackerRepository.getTracker(in));
      assertTrue(tracker.getDescriptor().matches("Unzipped .*\\.jar file org/junit/Test.class"));
    }
  }
}
