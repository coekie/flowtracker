package be.coekaerts.wouter.flowtracker.web;

import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.TreeResource.NodeRequestParams;
import be.coekaerts.wouter.flowtracker.web.TreeResource.NodeResponse;
import org.junit.Test;

public class TreeResourceTest {
  @Test
  public void test() {
    Node root = TrackerTree.node("TreeResourceTest.test");
    Node one = root.node("one");
    Node a = one.node("a");
    Node b = one.node("b");

    new ByteOriginTracker().addTo(a);
    new ByteOriginTracker().addTo(b);

    assertEquals(
        "TreeResourceTest.test\n"
            + "  one\n"
            + "    a\n"
            + "    b\n",
        toString(new NodeResponse(root, new NodeRequestParams(true, true))));
  }

  @Test
  public void testOptional() {
    Node root = TrackerTree.node("TreeResourceTest.testOptional");
    Node one = root.optionalNode("one");
    Node a = one.node("a");
    Node b = one.node("b");

    new ByteOriginTracker().addTo(a);
    new ByteOriginTracker().addTo(b);

    assertEquals(
        "TreeResourceTest.testOptional / one\n"
            + "  a\n"
            + "  b\n",
        toString(new NodeResponse(root, new NodeRequestParams(true, true))));
  }

  @Test
  public void testMultipleTrackersOnOneNode() {
    Node root = TrackerTree.node("TreeResourceTest.testMultipleTrackersOnOneNode");
    Node one = root.node("one");
    Node a = one.node("a");

    new ByteOriginTracker().addTo(a);
    new ByteOriginTracker().addTo(a);

    assertEquals(
        "TreeResourceTest.testMultipleTrackersOnOneNode\n"
            + "  one\n"
            + "    a\n"
            + "      0\n"
            + "      1\n",
        toString(new NodeResponse(root, new NodeRequestParams(true, true))));
  }


  @Test
  public void testFilter() {
    Node root = TrackerTree.node("TreeResourceTest.testFilter");
    Node one = root.optionalNode("one");
    Node a = one.node("aa").node("a");
    Node b = one.node("bb").node("b");

    new ByteOriginTracker().addTo(a);
    new ByteSinkTracker().addTo(b);

    assertEquals(
        "TreeResourceTest.testFilter / one\n"
            + "  aa\n"
            + "    a\n",
        toString(new NodeResponse(root, new NodeRequestParams(true, false))));
    assertEquals(
        "TreeResourceTest.testFilter / one\n"
            + "  bb\n"
            + "    b\n",
        toString(new NodeResponse(root, new NodeRequestParams(false, true))));
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