package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;

/** Reference to a Tracker */
@SuppressWarnings("UnusedDeclaration") // json
public class TrackerResponse {
  private final long id;
  private final String description;
  private final boolean origin;
  private final boolean sink;

  TrackerResponse(Tracker tracker) {
    id = tracker.getTrackerId();
    InterestRepository.register(tracker);
    description = tracker.getDescriptor();
    origin = TrackerResource.isOrigin(tracker);
    sink = TrackerResource.isSink(tracker);
  }

  public long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public boolean isOrigin() {
    return origin;
  }

  public boolean isSink() {
    return sink;
  }
}
