package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.Part;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilesTest {
  private static File file;

  @BeforeClass
  public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_InputStreamHookTest", "").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
  }

  @AfterClass
  public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  // this is not a test aimed at a specific hook
  @Test
  public void copy() throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    long result = Files.copy(file.toPath(), bout);
    assertEquals(3, result);
    Tracker tracker = requireNonNull(TrackerRepository.getTracker(bout.toByteArray()));
    TrackerSnapshot trackerSnapshot = TrackerSnapshot.of(tracker);
    assertEquals(1, trackerSnapshot.getParts().size());
    Part part = trackerSnapshot.getParts().get(0);
    assertTrue(part.source.getDescriptor().contains(file.getPath()));
  }
}
