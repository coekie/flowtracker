package be.coekaerts.wouter.flowtracker.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
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

  Map<String, String> snapshot(TrackerTree.Node node) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new Snapshot(node).write(baos);

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