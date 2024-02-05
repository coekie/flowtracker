package be.coekaerts.wouter.flowtracker.tracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tree of interesting trackers, for the UI */
public class TrackerTree {
  public static final Node ROOT = new Node(null, "<root>");

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
    private final Map<String, Node> children = new HashMap<>();
    private final List<Tracker> trackers = new ArrayList<>();

    Node(Node parent, String name) {
      this.parent = parent;
      this.name = name;
    }

    /** Get or create a new child node */
    public synchronized Node node(String name) {
      return children.computeIfAbsent(name, this::createChild);
    }

    private Node createChild(String name) {
      return new Node(this, name);
    }

    /** Should only be used by {@link Tracker#addTo(Node)} */
    synchronized void internalAddTracker(Tracker tracker) {
      trackers.add(tracker);
      tracker.initNode(this);
    }

    public synchronized Collection<Node> children() {
      return new ArrayList<>(children.values());
    }

    public synchronized Collection<Tracker> trackers() {
      return new ArrayList<>(trackers);
    }
  }
}
