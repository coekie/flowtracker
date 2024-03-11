package be.coekaerts.wouter.flowtracker.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import java.util.Arrays;
import org.junit.Test;

public class TrackerTreeTest {
  @Test
  public void testNode() {
    Node node = TrackerTree.node("foo");
    assertEquals(TrackerTree.ROOT, node.parent);
    assertEquals("foo", node.name);
    assertSame(TrackerTree.node("foo"), node);
  }

  @Test
  public void testFileNode() {
    assertEquals(Arrays.asList("Files", "foo", "bar"), TrackerTree.fileNode("/foo/bar").path());
    assertEquals(Arrays.asList("Files", "bar"), TrackerTree.fileNode("/foo/../bar").path());
    assertEquals(Arrays.asList("Files", "foo", "bar"), TrackerTree.fileNode("/foo//bar").path());
    assertEquals(Arrays.asList("Files", "foo", "bar"), TrackerTree.fileNode("/foo\\bar").path());
  }
}