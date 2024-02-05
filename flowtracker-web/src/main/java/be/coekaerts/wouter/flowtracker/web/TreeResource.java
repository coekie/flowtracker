package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerResponse;
import java.util.List;
import java.util.stream.Collectors;
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
  public static class NodeResponse {
    private final String name;
    private final List<NodeResponse> children;
    private final List<TrackerResponse> trackers;

    private NodeResponse(TrackerTree.Node node) {
      this.name = node.name;
      this.children = node.children().stream()
          .map(NodeResponse::new)
          .collect(Collectors.toList());
      this.trackers = node.trackers().stream()
          .map(TrackerResponse::new)
          .collect(Collectors.toList());
    }

    public String getName() {
      return name;
    }

    public List<NodeResponse> getChildren() {
      return children;
    }

    public List<TrackerResponse> getTrackers() {
      return trackers;
    }
  }
}
