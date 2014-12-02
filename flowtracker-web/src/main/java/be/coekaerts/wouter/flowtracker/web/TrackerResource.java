package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/tracker")
public class TrackerResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<TrackerResponse> get() {
    Collection<ContentTracker> trackers = InterestRepository.getContentTrackers();
    List<TrackerResponse> result = new ArrayList<>(trackers.size());
    for (ContentTracker tracker : trackers) {
      result.add(new TrackerResponse(tracker));
    }
    return result;
  }

  public static class TrackerResponse {
    private final long id;
    private final String description;

    public TrackerResponse(ContentTracker tracker) {
      id = tracker.getTrackerId();
      description = getRecursiveDescription(tracker);
    }

    public long getId() {
      return id;
    }

    public String getDescription() {
      return description;
    }
  }

  private static String getRecursiveDescription(Tracker tracker) {
    return tracker.getDescriptorTracker() == null ? tracker.getDescriptor()
        : tracker.getDescriptor() + " from "
            + getRecursiveDescription(tracker.getDescriptorTracker());
  }
}
