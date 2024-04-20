package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertThatTracker;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

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
    assertThat(file.delete()).isTrue();
  }

  // this is not a test aimed at a specific hook
  @Test
  public void copy() throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    long result = Files.copy(file.toPath(), bout);
    assertThat(result).isEqualTo(3);
    Tracker tracker = requireNonNull(TrackerRepository.getTracker(bout.toByteArray()));
    TrackerSnapshot trackerSnapshot = TrackerSnapshot.of(tracker);
    assertThat(trackerSnapshot.getParts()).hasSize(1);
    Part part = trackerSnapshot.getParts().get(0);
    assertThatTracker(part.source)
        .hasNodeStartingWith("Files")
        .hasNodeEndingWith(file.getName());
  }
}
