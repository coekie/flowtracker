package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/tree")
public class TreeResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public NodeResponse root(
      @QueryParam("origins") @DefaultValue("true") boolean origins,
      @QueryParam("sinks") @DefaultValue("true") boolean sinks) {
    return new NodeResponse(TrackerTree.ROOT, new NodeRequestParams(origins, sinks));
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class NodeResponse implements Comparable<NodeResponse> {
    private final String name;
    private final List<NodeResponse> children = new ArrayList<>();
    private final TrackerResponse tracker;
    private final int trackerCount;

    // Maps TrackerTree.Node to how we represent it in the UI. differences:
    // * Optional nodes are collapsed into their parent when they're the only child. That's to keep
    //   the UI minimal, avoid unnecessary deep nesting.
    // * every NodeResponse has at most one tracker associated. If there are multiple trackers under
    //   one TrackerTree.Node, then we create separate NodeResponses for them.
    @SuppressWarnings("StringConcatenationInLoop") // rarely loops
    NodeResponse(Node node, NodeRequestParams nodeRequestParams) {
      String name = node.name;
      while (node.children().size() == 1
          && node.trackers().isEmpty()
          && node.children().get(0).optional) {
        node = node.children().get(0);
        name = name + " / " + node.name;
      }
      this.name = name;
      int trackerCount = 0;
      for (Node child : node.children()) {
        NodeResponse childResponse = new NodeResponse(child, nodeRequestParams);
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
            this.children.add(new NodeResponse(Integer.toString(i), new TrackerResponse(tracker)));
            trackerCount++;
          }
        }
      }
      Collections.sort(this.children);
      this.trackerCount = trackerCount;
    }

    private NodeResponse(String name, TrackerResponse tracker) {
      this.name = name;
      this.tracker = tracker;
      this.trackerCount = 1;
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
