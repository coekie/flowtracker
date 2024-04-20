package be.coekaerts.wouter.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import org.junit.Test;

public class TrackerTreeTest {
  @Test
  public void testNode() {
    Node node = TrackerTree.node("foo");
    assertThat(node.parent).isEqualTo(TrackerTree.ROOT);
    assertThat(node.name).isEqualTo("foo");
    assertThat(node).isSameInstanceAs(TrackerTree.node("foo"));
  }

  @Test
  public void testFileNode() {
    assertThat(TrackerTree.fileNode("/foo/bar").path())
        .containsExactly("Files", "foo", "bar").inOrder();
    assertThat(TrackerTree.fileNode("/foo/../bar").path())
        .containsExactly("Files", "bar").inOrder();
    assertThat(TrackerTree.fileNode("/foo//bar").path())
        .containsExactly("Files", "foo", "bar").inOrder();
    assertThat(TrackerTree.fileNode("/foo\\bar").path())
        .containsExactly("Files", "foo", "bar").inOrder();
  }
}