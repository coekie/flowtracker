package com.coekie.flowtracker.web;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.coekie.flowtracker.web.SettingsResource.Settings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;

public class SnapshotTest {
  @Test
  public void testStatic() throws IOException {
    Node root = TrackerTree.node("SnapshotTest.testStatic");
    Map<String, String> entries = snapshot(root);
    assertThat(entries).containsKey("index.html");
    assertThat(entries.get("index.html")).contains("<script");
    assertThat(entries.keySet().stream().anyMatch(p -> p.startsWith("assets/index"))).isTrue();
  }

  @Test
  public void testGetStaticResources() throws IOException {
    // test that it works for files in the filesystem
    assertThat(Snapshot.findStaticResources("static", "index.html")).contains("folder.svg");
    // test that it works for files in jars (so that it _would_ work for static/index.html as well
    // when that is packaged in the flowtracker jar)
    assertThat(Snapshot.findStaticResources("jakarta/ws/rs", "GET.class")).contains("POST.class");
  }

  @Test
  public void testTree() throws IOException {
    Node root = TrackerTree.node("SnapshotTest.testTree");
    new ByteSinkTracker().addTo(root.node("mySink"));
    new ByteOriginTracker().addTo(root.node("myOrigin"));

    Map<String, String> entries = snapshot(root);
    assertThat(entries).containsKey("tree/all");
    assertThat(entries.get("tree/all")).contains("mySink");
    assertThat(entries.get("tree/all")).contains("myOrigin");

    assertThat(entries).containsKey("tree/sinks");
    assertThat(entries.get("tree/sinks")).contains("mySink");
    assertThat(entries.get("tree/sinks")).doesNotContain("myOrigin");

    assertThat(entries).containsKey("tree/origins");
    assertThat(entries.get("tree/origins")).doesNotContain("mySink");
    assertThat(entries.get("tree/origins")).contains("myOrigin");
  }

  @Test
  public void testTracker() throws IOException {
    Node root = TrackerTree.node("SnapshotTest.testTracker");
    ByteSinkTracker sink1 = new ByteSinkTracker();
    sink1.addTo(root.node("mySink"));
    ByteOriginTracker origin1 = new ByteOriginTracker();
    origin1.addTo(root.node("myOrigin"));

    // an origin that's not in the tree, but referenced from a tracker that is
    ByteOriginTracker origin2 = new ByteOriginTracker();

    sink1.setSource(0, 1, origin2, 0);
    sink1.append((byte) 1);

    Map<String, String> entries = snapshot(root);
    assertThat(entries).containsKey("tracker/" + sink1.getTrackerId());
    assertThat(entries).containsKey("tracker/" + origin1.getTrackerId());
    assertThat(entries).containsKey("tracker/" + origin2.getTrackerId());
    assertThat(entries)
        .containsKey("tracker/" + origin2.getTrackerId() + "_to_" + sink1.getTrackerId());
  }

  @Test
  public void testMinimize() throws IOException {
    Node root = TrackerTree.FILES; // minimizing only applies to FILES and CLASS
    ByteSinkTracker sink1 = new ByteSinkTracker();
    sink1.addTo(root.node("mySink"));
    ByteOriginTracker origin1 = new ByteOriginTracker();
    origin1.addTo(root.node("origin1"));

    // an origin that's not in the tree, but referenced from a tracker that is
    ByteOriginTracker origin2 = new ByteOriginTracker();

    // an origin in the tree, but not referenced from any tracker
    ByteOriginTracker origin3 = new ByteOriginTracker();
    origin3.addTo(root.node("origin3"));

    sink1.setSource(0, 1, origin1, 0);
    sink1.append((byte) 1);

    sink1.setSource(1, 1, origin2, 0);
    sink1.append((byte) 1);

    Map<String, String> entries = snapshot(root, true);
    assertThat(entries).containsKey("tracker/" + sink1.getTrackerId());
    assertThat(entries).containsKey("tracker/" + origin1.getTrackerId());
    assertThat(entries).containsKey("tracker/" + origin2.getTrackerId());
    assertThat(entries).doesNotContainKey("tracker/" + origin3.getTrackerId());
    assertThat(entries.get("tree/all")).contains("origin1");
    assertThat(entries.get("tree/all")).doesNotContain("origin3");
  }

  @Test
  public void testSettings() throws IOException {
    Map<String, String> entries = snapshot(TrackerTree.node("SnapshotTest.testSettings"));
    assertThat(entries).containsKey("settings");
    assertThat(Snapshot.GSON.fromJson(entries.get("settings"), Settings.class).snapshot).isTrue();
  }

  private Map<String, String> snapshot(TrackerTree.Node node) throws IOException {
    return snapshot(node, false);
  }

  private Map<String, String> snapshot(TrackerTree.Node node, boolean minimized)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new Snapshot(node, minimized).write(baos);

    Map<String, String> result = new HashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
        result.put(entry.getName().substring(Snapshot.PATH_PREFIX.length()),
            new String(zis.readAllBytes()));
      }
      zis.closeEntry();
    }
    return result;
  }
}