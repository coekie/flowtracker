package com.coekie.flowtracker.web;

import com.coekie.flowtracker.tracker.Tracker;
import java.util.List;

/** Reference to a Tracker */
@SuppressWarnings("UnusedDeclaration") // json
public class TrackerResponse {
  public final long id;
  public final List<String> path;
  public final boolean origin;
  public final boolean sink;

  TrackerResponse(Tracker tracker) {
    this.id = tracker.getTrackerId();
    this.path = tracker.getNode() == null ? null : tracker.getNode().path();
    this.origin = TrackerResource.isOrigin(tracker);
    this.sink = TrackerResource.isSink(tracker);
    InterestRepository.register(tracker);
  }
}
