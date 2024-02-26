package be.coekaerts.wouter.flowtracker.web;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.ByteContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.Growth;
import be.coekaerts.wouter.flowtracker.tracker.OriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.WritableTracker;
import java.util.ArrayList;
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
  @Path("{id}")
  public TrackerDetailResponse get(@PathParam("id") long id) {
    Tracker tracker = InterestRepository.getContentTracker(id);
    return new TrackerDetailResponse(tracker);
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class TrackerResponse {
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

  @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"}) // json
  public static class TrackerDetailResponse {
    private final List<Region> regions = new ArrayList<>();

    private TrackerDetailResponse(Tracker tracker) {
      if (tracker instanceof DefaultTracker) {
        tracker.pushSourceTo(0, tracker.getLength(), new WritableTracker() {
          @Override
          public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
              Growth growth) {
            String content = requireNonNull(getContentAsString(tracker, index, index + length));
            if (sourceTracker != null) {
              regions.add(new Region(content, singletonList(
                  new TrackerPartResponse(sourceTracker, sourceIndex, length))));
            } else {
              regions.add(new Region(content, emptyList()));
            }
          }
        }, 0);
      } else {
        regions.add(
            new Region(getContentAsString(tracker, 0, getContentLength(tracker)), emptyList()));
      }
    }

    public List<Region> getRegions() {
      return regions;
    }
  }

  public static class Region {
    private final String content;
    private final List<TrackerPartResponse> parts;

    public Region(String content, List<TrackerPartResponse> parts) {
      this.content = content;
      this.parts = parts;
    }

    public String getContent() {
      return content;
    }

    public List<TrackerPartResponse> getParts() {
      return parts;
    }
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class TrackerPartResponse {
    private final TrackerResponse tracker;
    private final int offset;
    private final int length;
    // TODO growth
    private final String context;

    public TrackerPartResponse(Tracker tracker, int offset, int length) {
      this.tracker = new TrackerResponse(tracker);
      this.offset = offset;
      this.length = length;
      this.context = getContentAsString(tracker,
          Math.max(0, offset - 10),
          Math.min(getContentLength(tracker), offset + length + 10));
    }

    public TrackerResponse getTracker() {
      return tracker;
    }

    public int getOffset() {
      return offset;
    }

    public String getContext() {
      return context;
    }
  }

  private static String getContentAsString(Tracker tracker, int start, int end) {
    if (tracker instanceof CharContentTracker) {
      CharContentTracker charTracker = (CharContentTracker) tracker;
      return charTracker.getContent().subSequence(start, end).toString();
    } else if (tracker instanceof ByteContentTracker) {
      ByteContentTracker byteTracker = (ByteContentTracker) tracker;
      // TODO encoding
      return new String(byteTracker.getContent().getByteContent().array(), start, end - start);
    } else {
      return null;
    }
  }

  private static int getContentLength(Tracker tracker) {
    if (tracker instanceof CharContentTracker) {
      CharContentTracker charTracker = (CharContentTracker) tracker;
      return charTracker.getContent().length();
    } else if (tracker instanceof ByteContentTracker) {
      ByteContentTracker byteTracker = (ByteContentTracker) tracker;
      return byteTracker.getContent().size();
    } else {
      return 0;
    }
  }

  static boolean isOrigin(Tracker tracker) {
    return tracker instanceof OriginTracker;
  }

  static boolean isSink(Tracker tracker) {
    return tracker instanceof ByteSinkTracker || tracker instanceof CharSinkTracker;
  }
}
