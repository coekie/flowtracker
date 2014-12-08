package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public TrackerDetailResponse get(@PathParam("id") long id) {
    ContentTracker tracker = InterestRepository.getContentTracker(id);
    return new TrackerDetailResponse(tracker.getContent().toString());
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

  public static class TrackerDetailResponse {
    private final String content;

    public TrackerDetailResponse(String content) {
      this.content = content;
    }

    public String getContent() {
      return content;
    }
  }
}
