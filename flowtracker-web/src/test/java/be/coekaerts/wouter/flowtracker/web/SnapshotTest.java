package be.coekaerts.wouter.flowtracker.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.SettingsResource.Settings;
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
    assertTrue(entries.containsKey("index.html"));
    assertTrue(entries.get("index.html").contains("<script"));
    assertTrue(entries.keySet().stream().anyMatch(p -> p.startsWith("assets/index")));
  }

  @Test
  public void testGetStaticResources() throws IOException {
    // test that it works for files in the filesystem
    assertTrue(Snapshot.findStaticResources("static", "index.html").contains("folder.svg"));
    // test that it works for files in jars (so that it _would_ work for static/index.html as well
    // when that is packaged in the flowtracker jar)
    assertTrue(Snapshot.findStaticResources("jakarta/ws/rs", "GET.class").contains("POST.class"));
  }

  @Test
  public void testTree() throws IOException {
    Node root = TrackerTree.node("SnapshotTest.testTree");
    new ByteSinkTracker().addTo(root.node("mySink"));
    new ByteOriginTracker().addTo(root.node("myOrigin"));

    Map<String, String> entries = snapshot(root);
    assertTrue(entries.containsKey("tree/all"));
    assertTrue(entries.get("tree/all").contains("mySink"));
    assertTrue(entries.get("tree/all").contains("myOrigin"));

    assertTrue(entries.containsKey("tree/sinks"));
    assertTrue(entries.get("tree/sinks").contains("mySink"));
    assertFalse(entries.get("tree/sinks").contains("myOrigin"));

    assertTrue(entries.containsKey("tree/origins"));
    assertFalse(entries.get("tree/origins").contains("mySink"));
    assertTrue(entries.get("tree/origins").contains("myOrigin"));
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
    assertTrue(entries.containsKey("tracker/" + sink1.getTrackerId()));
    assertTrue(entries.containsKey("tracker/" + origin1.getTrackerId()));
    assertTrue(entries.containsKey("tracker/" + origin2.getTrackerId()));
    assertTrue(entries.containsKey("tracker/"
        + origin2.getTrackerId() + "_to_" + sink1.getTrackerId()));
  }

  @Test
  public void testMinimize() throws IOException {
    Node root = TrackerTree.node("SnapshotTest.testMinimize");
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
    assertTrue(entries.containsKey("tracker/" + sink1.getTrackerId()));
    assertTrue(entries.containsKey("tracker/" + origin1.getTrackerId()));
    assertTrue(entries.containsKey("tracker/" + origin2.getTrackerId()));
    assertFalse(entries.containsKey("tracker/" + origin3.getTrackerId()));
    assertTrue(entries.get("tree/all").contains("origin1"));
    assertFalse(entries.get("tree/all").contains("origin3"));
  }

  @Test
  public void testSettings() throws IOException {
    Map<String, String> entries = snapshot(TrackerTree.node("SnapshotTest.testSettings"));
    assertTrue(entries.containsKey("settings"));
    assertTrue(Snapshot.GSON.fromJson(entries.get("settings"), Settings.class).snapshot);
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