package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import com.coekie.flowtracker.tracker.TrackerSnapshot.Part;
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
    assertThatTrackerNode(part.source)
        .hasPathStartingWith("Files")
        .hasPathEndingWith(file.getName());
  }
}
