package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/tree")
public class TreeResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public NodeResponse root() {
    return new NodeResponse(TrackerTree.ROOT);
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class NodeResponse implements Comparable<NodeResponse> {
    private final String name;
    private final List<NodeResponse> children = new ArrayList<>();
    private final TrackerResponse tracker;

    // Maps TrackerTree.Node to how we represent it in the UI. differences:
    // * Optional nodes are collapsed into their parent when they're the only child. That's to keep
    //   the UI minimal, avoid unnecessary deep nesting.
    // * every NodeResponse has at most one tracker associated. If there are multiple trackers under
    //   one TrackerTree.Node, then we create separate NodeResponses for them.
    @SuppressWarnings("StringConcatenationInLoop") // rarely loops
    NodeResponse(TrackerTree.Node node) {
      String name = node.name;
      while (node.children().size() == 1
          && node.trackers().isEmpty()
          && node.children().get(0).optional) {
        node = node.children().get(0);
        name = name + " / " + node.name;
      }
      this.name = name;
      for (Node child : node.children()) {
        this.children.add(new NodeResponse(child));
      }
      List<Tracker> trackers = node.trackers();
      if (trackers.size() == 1 && this.children.isEmpty()) {
        this.tracker = new TrackerResponse(trackers.get(0));
      } else {
        this.tracker = null;
        for (int i = 0; i < trackers.size(); i++) {
          Tracker tracker = trackers.get(i);
          this.children.add(new NodeResponse(Integer.toString(i), new TrackerResponse(tracker)));
        }
      }
      Collections.sort(this.children);
    }

    private NodeResponse(String name, TrackerResponse tracker) {
      this.name = name;
      this.tracker = tracker;
    }

    public String getName() {
      return name;
    }

    public List<NodeResponse> getChildren() {
      return children;
    }

    public TrackerResponse getTracker() {
      return tracker;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof NodeResponse)) {
        return false;
      }
      NodeResponse that = (NodeResponse) o;
      return name.equals(that.name) && children.equals(that.children)
          && Objects.equals(tracker, that.tracker);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, children, tracker);
    }

    @Override
    public int compareTo(NodeResponse o) {
      return name.compareTo(o.name);
    }
  }
}
