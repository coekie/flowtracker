package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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

    private TrackerDetailResponse(Tracker tracker) {
      if (tracker instanceof DefaultTracker) {
        // combine single content + part trackers into list of TrackerPartResponses, where each one
        // has its own piece of the content, and adding parts for the gaps in between parts
        NavigableMap<Integer, PartTracker> map = ((DefaultTracker) tracker).getMap();
        int pos = 0;
        for (Map.Entry<Integer, PartTracker> entry : map.entrySet()) {
          Integer partPos = entry.getKey();
          PartTracker part = entry.getValue();

          // gap before this entry
          if (partPos != pos) {
            String gapContent = tracker.getContent().subSequence(pos, partPos).toString();
            parts.add(new TrackerPartResponse(gapContent));
          }

          // this entry
          String content = tracker.getContent()
              .subSequence(partPos, partPos + part.getLength())
              .toString();
          parts.add(new TrackerPartResponse(content, part));

          pos = partPos + part.getLength();
        }
        // gap at the end
        if (pos != tracker.getContent().length()) {
          String gapContent =
              tracker.getContent().subSequence(pos, tracker.getContent().length()).toString();
          parts.add(new TrackerPartResponse(gapContent));
        }
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

    public TrackerPartResponse(String content, PartTracker part) {
      this.content = content;
      this.source = new TrackerResponse(part.getTracker());
      this.sourceOffset = part.getIndex();
      if (part.getTracker().supportsContent()) {
        CharSequence sourceContent = part.getTracker().getContent();
        this.sourceContext = sourceContent.subSequence(Math.max(0, sourceOffset - 10),
            Math.min(sourceContent.length(), sourceOffset + part.getLength() + 10)).toString();
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

    // not a real getter because it causes performance issues
    // rename to getSourceContext to see the effect
    public String doGetSourceContext() {
      return sourceContext;
    }
  }
}
