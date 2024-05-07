package be.coekaerts.wouter.flowtracker.web;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import be.coekaerts.wouter.flowtracker.tracker.ByteContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Growth;
import be.coekaerts.wouter.flowtracker.tracker.OriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Simplifier;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.tracker.WritableTracker;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

@Path("/tracker")
public class TrackerResource {

  /**
   * Returns the content of tracker `id`, indicating where each region comes from.
   * <p>
   * The regions returned here always have at most one {@link Region#parts part}.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public TrackerDetailResponse get(@PathParam("id") long id) {
    Tracker tracker = InterestRepository.getContentTracker(id);
    List<Region> regions = new ArrayList<>();
    if (tracker instanceof DefaultTracker) {
      Simplifier.simplifySourceTo(tracker, new WritableTracker() {
        @Override
        public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
            Growth growth) {
          if (sourceTracker != null) {
            regions.add(new Region(tracker, index, length, singletonList(
                new TrackerPartResponse(sourceTracker, sourceIndex,
                    growth.targetToSource(length)))));
          } else {
            regions.add(new Region(tracker, index, length, emptyList()));
          }
        }
      });
    } else {
      regions.add(
          new Region(tracker, 0, getContentLength(tracker), emptyList()));
    }
    return new TrackerDetailResponse(tracker, regions);
  }

  /**
   * Returns contents of tracker `id`, indicating which regions in it ended up in `target`.
   * This indicates where the data in this tracker went.
   * This is the reverse of {@link #get(long)}, which indicates where the data in the given tracker
   * came from.
   * <p>
   * One region here can contain multiple parts (if a part of the content of `id` is copied into
   * `target` multiple times).
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}_to_{target}")
  public TrackerDetailResponse reverse(@PathParam("id") long id,
      @PathParam("target") long targetId) {
    Tracker tracker = InterestRepository.getContentTracker(id);
    Tracker target = InterestRepository.getContentTracker(targetId);

    List<Region> regions = new ArrayList<>();

    List<TrackerPartResponse> activeParts = new ArrayList<>();

    // record at which indexes in tracker that changes happen to which parts correspond to it.
    // a change means the start or end of an associated part.
    // the Runnable in this map mutates activeParts with the relevant change.
    // in other words, this iterates over the content of `target`, and builds an ~index of how that
    // maps to indexes in the content of `tracker`
    TreeMap<Integer, List<Runnable>> changePoints = new TreeMap<>();
    changePoints.put(0, new ArrayList<>());
    Simplifier.simplifySourceTo(target, new WritableTracker() {
      @Override
      public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
          Growth growth) {
        int sourceLength = growth.targetToSource(length);
        if (sourceTracker == tracker) {
          TrackerPartResponse part = new TrackerPartResponse(target, index, length);
          changePoints.computeIfAbsent(sourceIndex, i -> new ArrayList<>())
              .add(() -> activeParts.add(part));
          changePoints.computeIfAbsent(sourceIndex + sourceLength, i -> new ArrayList<>())
              .add(() -> activeParts.remove(part));
        } else if (sourceTracker != null && sourceTracker.getEntryCount() > 0) {
          // recurse to the source of the source
          sourceTracker.pushSourceTo(sourceIndex, sourceLength, this, index, growth);
        }
      }
    });

    // iterate of the content of `tracker`, building up regions.
    for (int i : changePoints.keySet()) {
      // update activeParts to match the parts active at i
      for (Runnable change : changePoints.get(i)) {
        change.run();
      }

      // the end of this region is where the next one begins,
      // or else (for the last region) the end of `tracker`.
      Integer ceil = changePoints.ceilingKey(i + 1);
      int endIndex = ceil == null ? getContentLength(tracker) : ceil;

      // if-condition: don't write out empty region that can otherwise appear at the end
      if (endIndex <= i) {
        break;
      }

      regions.add(new Region(tracker, i, endIndex - i, new ArrayList<>(activeParts)));
    }

    return new TrackerDetailResponse(tracker, regions);
  }

  @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"}) // json
  public static class TrackerDetailResponse {
    public final List<String> path;
    public final String creationStackTrace;
    public final List<Region> regions;

    private TrackerDetailResponse(Tracker tracker, List<Region> regions) {
      this.path = path(tracker);
      this.creationStackTrace = creationStackTraceToString(tracker);
      this.regions = regions;
    }
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class Region {
    public final int offset;
    public final int length;
    public final String content;
    public final List<TrackerPartResponse> parts;

    Region(Tracker tracker, int offset, int length, List<TrackerPartResponse> parts) {
      this.offset = offset;
      this.length =length;
      this.content = getContentAsString(tracker, offset, offset + length);
      this.parts = parts;
    }
  }

  @SuppressWarnings("UnusedDeclaration") // json
  public static class TrackerPartResponse {
    public final TrackerResponse tracker;
    public final int offset;
    public final int length;

    public TrackerPartResponse(Tracker tracker, int offset, int length) {
      this.tracker = new TrackerResponse(tracker);
      this.offset = offset;
      this.length = length;
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
    } else if (tracker instanceof FixedOriginTracker) {
      return "<not implemented>";
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
    } else if (tracker instanceof FixedOriginTracker) {
      return tracker.getLength();
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

  private static List<String> path(Tracker tracker) {
    if (tracker.getNode() == null) {
      return null;
    } else {
      return path(tracker.getNode());
    }
  }

  private static List<String> path(Node node) {
    ArrayList<String> result = new ArrayList<>();
    for (Node n = node; n != TrackerTree.ROOT; n = n.parent) {
      result.add(n.name);
    }
    Collections.reverse(result);
    return result;
  }

  private static String creationStackTraceToString(Tracker tracker) {
    StackTraceElement[] trace = tracker.getCreationStackTrace();
    if (trace == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (StackTraceElement element : trace) {
      sb.append("at ").append(element).append('\n');
    }
    return sb.toString();
  }
}
