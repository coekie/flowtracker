package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.WritableTracker;
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
  public List<TrackerResponse> list() {
    Collection<Tracker> trackers = InterestRepository.getTrackers();
    List<TrackerResponse> result = new ArrayList<>(trackers.size());
    for (Tracker tracker : trackers) {
      result.add(new TrackerResponse(tracker));
    }
    return result;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public TrackerDetailResponse get(@PathParam("id") long id) {
    Tracker tracker = InterestRepository.getContentTracker(id);
    return new TrackerDetailResponse(tracker);
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class TrackerResponse {
    private final long id;
    private final String description;

    private TrackerResponse(Tracker tracker) {
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

  @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"}) // json
  public static class TrackerDetailResponse {
    private final List<TrackerPartResponse> parts = new ArrayList<>();

    private TrackerDetailResponse(final Tracker tracker) {
      if (tracker instanceof DefaultTracker) {
        tracker.pushContentToTracker(0, tracker.getLength(), new WritableTracker() {
          @Override
          public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex) {
            String content = tracker.getContent().subSequence(index, index + length).toString();
            if (sourceTracker != null) {
              parts.add(new TrackerPartResponse(content, length, sourceTracker, sourceIndex));
            } else {
              parts.add(new TrackerPartResponse(content));
            }
          }
        }, 0);
      } else {
        parts.add(new TrackerPartResponse(tracker.getContent().toString()));
      }
    }

    public List<TrackerPartResponse> getParts() {
      return parts;
    }
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class TrackerPartResponse {
    private final String content;
    private final TrackerResponse source;
    private final int sourceOffset;
    private final String sourceContext;

    public TrackerPartResponse(String content) {
      this.content = content;
      this.source = null;
      this.sourceOffset = -1;
      this.sourceContext = null;
    }

    public TrackerPartResponse(String content, int length, Tracker sourceTracker, int sourceIndex) {
      this.content = content;
      this.source = new TrackerResponse(sourceTracker);
      this.sourceOffset = sourceIndex;
      if (sourceTracker.supportsContent()) {
        CharSequence sourceContent = sourceTracker.getContent();
        this.sourceContext = sourceContent.subSequence(Math.max(0, sourceOffset - 10),
            Math.min(sourceContent.length(), sourceOffset + length + 10)).toString();
      } else {
        this.sourceContext = null;
      }
    }

    public String getContent() {
      return content;
    }

    public TrackerResponse getSource() {
      return source;
    }

    public int getSourceOffset() {
      return sourceOffset;
    }

    public String getSourceContext() {
      return sourceContext;
    }
  }
}
