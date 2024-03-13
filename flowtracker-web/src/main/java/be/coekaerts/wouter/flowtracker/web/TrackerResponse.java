package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import java.util.List;

/** Reference to a Tracker */
@SuppressWarnings("UnusedDeclaration") // json
public class TrackerResponse {
  private final Tracker tracker;

  TrackerResponse(Tracker tracker) {
    this.tracker = tracker;
    InterestRepository.register(tracker);
  }

  public long getId() {
    return tracker.getTrackerId();
  }

  public String getDescription() {
    return tracker.getDescriptor();
  }

  public List<String> getPath() {
    Node node = tracker.getNode();
    return node == null ? null : node.path();
  }

  public boolean isOrigin() {
    return TrackerResource.isOrigin(tracker);
  }

  public boolean isSink() {
    return TrackerResource.isSink(tracker);
  }
}
