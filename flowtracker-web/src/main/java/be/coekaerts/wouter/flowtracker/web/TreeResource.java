package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Path("/tree")
public class TreeResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public NodeDetailResponse root(
      @QueryParam("origins") @DefaultValue("true") boolean origins,
      @QueryParam("sinks") @DefaultValue("true") boolean sinks) {
    return new NodeDetailResponse(TrackerTree.ROOT, new NodeRequestParams(origins, sinks));
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class NodeDetailResponse implements Comparable<NodeDetailResponse> {
    private final List<String> names = new ArrayList<>();
    private final List<NodeDetailResponse> children = new ArrayList<>();
    private final TrackerResponse tracker;
    private final int trackerCount;

    // Maps TrackerTree.Node to how we represent it in the UI. differences:
    // * Optional nodes are collapsed into their parent when they're the only child. That's to keep
    //   the UI minimal, avoid unnecessary deep nesting.
    // * every NodeDetailResponse has at most one tracker associated. If there are multiple trackers
    //   under one TrackerTree.Node, then we create separate NodeDetailResponses for them.
    NodeDetailResponse(Node node, NodeRequestParams nodeRequestParams) {
      names.add(node.name);
      while (node.children().size() == 1
          && node.trackers().isEmpty()
          && node.children().get(0).optional) {
        node = node.children().get(0);
        names.add(node.name);
      }
      int trackerCount = 0;
      for (Node child : node.children()) {
        NodeDetailResponse childResponse = new NodeDetailResponse(child, nodeRequestParams);
        if (childResponse.trackerCount > 0) {
          trackerCount += childResponse.trackerCount;
          this.children.add(childResponse);
        }
      }
      List<Tracker> trackers = node.trackers();
      if (trackers.size() == 1 && this.children.isEmpty()) {
        if (nodeRequestParams.include(trackers.get(0))) {
          this.tracker = new TrackerResponse(trackers.get(0));
          trackerCount++;
        } else {
          this.tracker = null;
        }
      } else {
        this.tracker = null;
        for (int i = 0; i < trackers.size(); i++) {
          Tracker tracker = trackers.get(i);
          if (nodeRequestParams.include(tracker)) {
            this.children.add(new NodeDetailResponse(Integer.toString(i), new TrackerResponse(tracker)));
            trackerCount++;
          }
        }
      }
      Collections.sort(this.children);
      this.trackerCount = trackerCount;
    }

    private NodeDetailResponse(String name, TrackerResponse tracker) {
      this.names.add(name);
      this.tracker = tracker;
      this.trackerCount = 1;
    }

    public List<String> getNames() {
      return names;
    }

    public List<NodeDetailResponse> getChildren() {
      return children;
    }

    public TrackerResponse getTracker() {
      return tracker;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof NodeDetailResponse)) {
        return false;
      }
      NodeDetailResponse that = (NodeDetailResponse) o;
      return names.equals(that.names) && children.equals(that.children)
          && Objects.equals(tracker, that.tracker);
    }

    @Override
    public int hashCode() {
      return Objects.hash(names, children, tracker);
    }

    @Override
    public int compareTo(NodeDetailResponse o) {
      int commonNameLen = Math.min(names.size(), o.names.size());
      for (int i = 0; i < commonNameLen; i++) {
        int result = names.get(i).compareTo(o.names.get(i));
        if (result != 0) {
          return result;
        }
      }
      return Integer.compare(names.size(), o.names.size());
    }
  }

  static class NodeRequestParams {
    /** Include origins in the response */
    private final boolean origins;

    /** Include sinks in the response */
    private final boolean sinks;

    NodeRequestParams(boolean origins, boolean sinks) {
      this.origins = origins;
      this.sinks = sinks;
    }

    boolean include(Tracker tracker) {
      return (origins && TrackerResource.isOrigin(tracker))
          || (sinks && TrackerResource.isSink(tracker));
    }
  }
}
