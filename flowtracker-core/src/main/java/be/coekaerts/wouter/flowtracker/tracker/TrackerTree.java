package be.coekaerts.wouter.flowtracker.tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tree of interesting trackers, for the UI */
public class TrackerTree {
  public static final Node ROOT = new Node(null, "<root>", false);

  public static Node node(String name) {
    return ROOT.node(name);
  }

  /** Root node for tracking operations on files */
  public static Node fileNode(String name) {
    return node("Files").node(name);
  }

  public static Node nodeOrUnknown(Tracker tracker) {
    Node node;
    if (tracker == null || (node = tracker.getNode()) == null) {
      return node("Unknown");
    } else {
      return node;
    }
  }

  public static class Node {
    public final Node parent;
    public final String name;

    /**
     * An optional node is hidden if it doesn't have siblings; only shown if necessary for
     * disambiguation
     */
    public final boolean optional;

    private final Map<String, Node> children = new HashMap<>();
    private final List<Tracker> trackers = new ArrayList<>();

    Node(Node parent, String name, boolean optional) {
      this.parent = parent;
      this.name = name;
      this.optional = optional;
    }

    /** Get or create a new child node */
    public synchronized Node node(String name) {
      return children.computeIfAbsent(name, this::createChild);
    }

    /** Get or create a new optional child node */
    public synchronized Node optionalNode(String name) {
      return children.computeIfAbsent(name, this::createOptionalChild);
    }

    private Node createChild(String name) {
      return new Node(this, name, false);
    }

    private Node createOptionalChild(String name) {
      return new Node(this, name, true);
    }

    /** Should only be used by {@link Tracker#addTo(Node)} */
    synchronized void internalAddTracker(Tracker tracker) {
      trackers.add(tracker);
      tracker.initNode(this);
    }

    public synchronized List<Node> children() {
      return new ArrayList<>(children.values());
    }

    public synchronized List<Tracker> trackers() {
      return new ArrayList<>(trackers);
    }
  }
}
