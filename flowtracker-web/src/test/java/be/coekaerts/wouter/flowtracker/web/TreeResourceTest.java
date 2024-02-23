package be.coekaerts.wouter.flowtracker.web;

import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.TreeResource.NodeResponse;
import org.junit.Test;

public class TreeResourceTest {
  @Test
  public void test() {
    Node root = TrackerTree.node("TreeResourceTest.test");
    Node one = root.node("one");
    one.node("a");
    one.node("b");

    assertEquals(
        "TreeResourceTest.test\n"
            + "  one\n"
            + "    a\n"
            + "    b\n",
        toString(new NodeResponse(root)));
  }

  @Test
  public void testOptional() {
    Node root = TrackerTree.node("TreeResourceTest.testOptional");
    Node one = root.optionalNode("one");
    one.node("a");
    one.node("b");

    assertEquals(
        "TreeResourceTest.testOptional / one\n"
            + "  a\n"
            + "  b\n",
        toString(new NodeResponse(root)));
  }

  private String toString(NodeResponse nodeResponse) {
    return toString(nodeResponse, "");
  }

  private String toString(NodeResponse nodeResponse, String indent) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent).append(nodeResponse.getName()).append('\n');
    for (NodeResponse child : nodeResponse.getChildren()) {
      sb.append(toString(child, indent + "  "));
    }
    return sb.toString();
  }
}