package com.coekie.flowtracker.web;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.coekie.flowtracker.web.TreeResource.NodeDetailResponse;
import com.coekie.flowtracker.web.TreeResource.NodeRequestParams;
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

    assertThat(toString(new NodeDetailResponse(root, NodeRequestParams.ALL))).isEqualTo(
        "TreeResourceTest.test\n"
            + "  one\n"
            + "    a\n"
            + "    b\n");
  }

  @Test
  public void testOptional() {
    Node root = TrackerTree.node("TreeResourceTest.testOptional");
    Node one = root.optionalNode("one");
    Node a = one.node("a");
    Node b = one.node("b");

    new ByteOriginTracker().addTo(a);
    new ByteOriginTracker().addTo(b);

    assertThat(toString(new NodeDetailResponse(root, NodeRequestParams.ALL))).isEqualTo(
        "TreeResourceTest.testOptional / one\n"
            + "  a\n"
            + "  b\n");
  }

  @Test
  public void testMultipleTrackersOnOneNode() {
    Node root = TrackerTree.node("TreeResourceTest.testMultipleTrackersOnOneNode");
    Node one = root.node("one");
    Node a = one.node("a");

    new ByteOriginTracker().addTo(a);
    new ByteOriginTracker().addTo(a);

    assertThat(toString(new NodeDetailResponse(root, NodeRequestParams.ALL))).isEqualTo(
        "TreeResourceTest.testMultipleTrackersOnOneNode\n"
            + "  one\n"
            + "    a\n"
            + "      0\n"
            + "      1\n");
  }


  @Test
  public void testFilter() {
    Node root = TrackerTree.node("TreeResourceTest.testFilter");
    Node one = root.optionalNode("one");
    Node a = one.node("aa").node("a");
    Node b = one.node("bb").node("b");

    new ByteOriginTracker().addTo(a);
    new ByteSinkTracker().addTo(b);

    assertThat(toString(new NodeDetailResponse(root, NodeRequestParams.ORIGINS))).isEqualTo(
        "TreeResourceTest.testFilter / one\n"
            + "  aa\n"
            + "    a\n");
    assertThat(toString(new NodeDetailResponse(root, NodeRequestParams.SINKS))).isEqualTo(
        "TreeResourceTest.testFilter / one\n"
            + "  bb\n"
            + "    b\n");
  }

  private String toString(NodeDetailResponse nodeDetailResponse) {
    return toString(nodeDetailResponse, "");
  }

  private String toString(NodeDetailResponse nodeDetailResponse, String indent) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent).append(String.join(" / ", nodeDetailResponse.names)).append('\n');
    for (NodeDetailResponse child : nodeDetailResponse.children) {
      sb.append(toString(child, indent + "  "));
    }
    return sb.toString();
  }
}