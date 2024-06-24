package com.coekie.flowtracker.web;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.ByteContentTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.CharContentTracker;
import com.coekie.flowtracker.tracker.CharSinkTracker;
import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.DefaultTracker;
import com.coekie.flowtracker.tracker.FakeOriginTracker;
import com.coekie.flowtracker.tracker.Growth;
import com.coekie.flowtracker.tracker.OriginTracker;
import com.coekie.flowtracker.tracker.Simplifier;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.coekie.flowtracker.tracker.WritableTracker;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Tracker tracker = requireNonNull(InterestRepository.getContentTracker(id));
    List<Region> regions = new ArrayList<>();
    TrackerPartResponseBuilder partBuilder = new TrackerPartResponseBuilder();
    if (tracker instanceof DefaultTracker) {
      Simplifier.simplifySourceTo(tracker, (index, length, sourceTracker, sourceIndex, growth) -> {
        if (sourceTracker != null) {
          regions.add(new Region(tracker, index, length, singletonList(
              partBuilder.part(sourceTracker, sourceIndex, growth.targetToSource(length)))));
        } else {
          regions.add(new Region(tracker, index, length, emptyList()));
        }
      });
    } else {
      regions.add(
          new Region(tracker, 0, getContentLength(tracker), emptyList()));
    }
    boolean hasSource = false; // line mapping not implemented yet (only in `reverse`)
    return new TrackerDetailResponse(tracker, regions, partBuilder, hasSource);
  }

  /** @see #reverse(long, long, boolean)  */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}_to_{target}")
  public TrackerDetailResponse reverse(@PathParam("id") long id,
      @PathParam("target") long targetId) {
    return reverse(id, targetId, false);
  }

  /**
   * Returns contents of tracker `id`, indicating which regions in it ended up in `target`.
   * This indicates where the data in this tracker went.
   * This is the reverse of {@link #get(long)}, which indicates where the data in the given tracker
   * came from.
   * <p>
   * One region here can contain multiple parts (if a part of the content of `id` is copied into
   * `target` multiple times).
   * <p>
   * If `includeParts` is false, the parts are not returned (because currently the UI doesn't
   * actually use them). But the regions are still split up in a way where when the parts that
   * reference the tracker change it creates a new region.
   */
  public TrackerDetailResponse reverse(long id, long targetId, boolean includeParts) {
    Tracker tracker = InterestRepository.getContentTracker(id);
    Tracker target = InterestRepository.getContentTracker(targetId);

    List<Region> regions = new ArrayList<>();
    TrackerPartResponseBuilder partBuilder = new TrackerPartResponseBuilder();

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
          TrackerPartResponse part = partBuilder.part(target, index, length);
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

    // similarly, for ClassOriginTracker, track line number changes
    int[] activeLineNumber = {-1}; // using array as a mutable int.
    if (tracker instanceof ClassOriginTracker) {
      ((ClassOriginTracker) tracker).pushLineNumbers((start, end, line) -> {
        changePoints.computeIfAbsent(start, i -> new ArrayList<>())
            .add(() -> activeLineNumber[0] = line);
        changePoints.computeIfAbsent(end, i -> new ArrayList<>())
            .add(() -> activeLineNumber[0] = -1);
      });
    }

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

      regions.add(new Region(tracker, i, endIndex - i,
          includeParts ? new ArrayList<>(activeParts) : List.of(),
          activeLineNumber[0]));
    }

    return new TrackerDetailResponse(tracker, regions, partBuilder,
        tracker instanceof ClassOriginTracker);
  }

  /**
   * Detailed view of a tracker, including its contents (splits into regions) and how those regions
   * link to other trackers.
   */
  @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"}) // json
  public static class TrackerDetailResponse {
    public final List<String> path;
    public final String creationStackTrace;
    public final List<Region> regions;

    /**
     * Info about each tracker that is referenced in any {@link TrackerPartResponse#trackerId} in
     * {@link Region#parts} in {@link #regions}.
     * Collected separately in a map here (instead of inline in {@link TrackerPartResponse}) to
     * minimize the size of the response; as the same tracker is often referenced many times.
     */
    public final Map<Long, TrackerResponse> linkedTrackers;

    /**
     * For {@link ClassOriginTracker}, indicates if there is associated source code.
     * @see Region#line
     */
    public final boolean hasSource;

    private TrackerDetailResponse(Tracker tracker, List<Region> regions,
        TrackerPartResponseBuilder partBuilder, boolean hasSource) {
      this.path = path(tracker);
      this.creationStackTrace = creationStackTraceToString(tracker);
      this.regions = regions;
      this.linkedTrackers = partBuilder.linkedTrackers;
      this.hasSource = hasSource;
    }
  }

  /** Part of the content of a tracker that can be related to other trackers */
  @SuppressWarnings("UnusedDeclaration") // json
  public static class Region {
    /** Offset in the contents of the complete tracker, where this region begins */
    public final int offset;

    /**
     * Length of this region, in the same ~units as {@link #offset}. That is, the offset of the
     * next region is the offset+length of this one. This is often but not always the same as
     * {@code content.length()}: it is e.g. different when dealing with {@link #escape(ByteBuffer)
     * escaping}).
     */
    public final int length;

    public final String content;

    /** Relation to other trackers (where its contents came from or went to) */
    public final List<TrackerPartResponse> parts;

    /** For {@link ClassOriginTracker}, indicates the line number in the source code */
    public final Integer line;

    Region(Tracker tracker, int offset, int length, List<TrackerPartResponse> parts) {
      this.offset = offset;
      this.length = length;
      this.content = getContentAsString(tracker, offset, offset + length);
      this.parts = parts;
      this.line = null;
    }

    Region(Tracker tracker, int offset, int length, List<TrackerPartResponse> parts, int line) {
      this.offset = offset;
      this.length = length;
      this.content = getContentAsString(tracker, offset, offset + length);
      this.parts = parts;
      this.line = line == -1 ? null : line;
    }
  }

  /** Relation between a {@link Region} of one tracker to a region in another tracker */
  @SuppressWarnings("UnusedDeclaration") // json
  public static class TrackerPartResponse {
    public final long trackerId;
    public final int offset;
    public final int length;

    public TrackerPartResponse(Tracker tracker, int offset, int length) {
      this.trackerId = tracker.getTrackerId();
      this.offset = offset;
      this.length = length;
    }
  }

  /**
   * Builds a {@link TrackerPartResponse}, making sure every linked tracker is in the
   * {@link #linkedTrackers} map
   */
  static class TrackerPartResponseBuilder {
    private final Map<Long, TrackerResponse> linkedTrackers = new HashMap<>();

    public TrackerPartResponse part(Tracker tracker, int offset, int length) {
      if (!linkedTrackers.containsKey(tracker.getTrackerId())) {
        linkedTrackers.put(tracker.getTrackerId(), new TrackerResponse(tracker));
      }
      return new TrackerPartResponse(tracker, offset, length);
    }
  }

  private static String getContentAsString(Tracker tracker, int start, int end) {
    if (tracker instanceof CharContentTracker) {
      CharContentTracker charTracker = (CharContentTracker) tracker;
      return charTracker.getContent().subSequence(start, end).toString();
    } else if (tracker instanceof ByteContentTracker) {
      ByteContentTracker byteTracker = (ByteContentTracker) tracker;
      ByteBuffer slice = byteTracker.getContent().getByteContent().slice();
      if (end > slice.limit()) {
        // this probably means we have tracking on a part where we did not record the content for.
        // that shouldn't happen; but it at least does in some unit tests where we were too lazy to
        // populate content.
        return "<invalid>";
      }
      slice.position(start);
      slice.limit(end);
      return escape(slice);
    } else if (tracker instanceof FakeOriginTracker) {
      return "<fake>";
    } else {
      return null;
    }
  }

  static int getContentLength(Tracker tracker) {
    if (tracker instanceof CharContentTracker) {
      CharContentTracker charTracker = (CharContentTracker) tracker;
      return charTracker.getContent().length();
    } else if (tracker instanceof ByteContentTracker) {
      ByteContentTracker byteTracker = (ByteContentTracker) tracker;
      return byteTracker.getContent().size();
    } else if (tracker instanceof FakeOriginTracker) {
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

  /**
   * Convert a ByteBuffer to a String, how we render those bytes in the UI.
   * <p>
   * Concretely, anything that's not a printable ascii character is shown as hex values, surrounded
   * by ❲ and ❳
   */
  private static String escape(ByteBuffer buf) {
    StringBuilder result = new StringBuilder(buf.limit() - buf.position());
    boolean escaped = false;
    boolean hadNonNewline = false; // have we seen anything besides newlines
    for (int i = buf.position(); i < buf.limit(); i++) {
      int b = buf.get(i) & 0xff;
      if ((b >= 32 && b < 127) || b == '\n' || b == '\r') { // printable ascii characters
        if (b == '\n' || b == '\r') {
          // if the first (possibly the only) character is a newline, then prefix it with ⏎, because
          // otherwise it's hard/confusing to select that part in the UI, because there's nothing to
          // click on.
          if (!hadNonNewline) {
            result.append("⏎");
            hadNonNewline = true;
          }
        } else {
          hadNonNewline = true;
        }
        if (escaped) {
          result.append('❳'); // close them if they were open
          escaped = false;
        }
        result.append((char) b);
      } else {
        if (escaped) {
          result.append(' '); // multiple escaped chars in a row are delimited by space
        } else {
          result.append('❲'); // open them if they weren't already
          escaped = true;
        }
        // an escaped byte is rendered as two 0-F chars
        appendHex(result, b / 16);
        appendHex(result, b % 16);
        hadNonNewline = true;
      }
    }
    if (escaped) {
      result.append('❳');
    }
    return result.toString();
  }

  private static void appendHex(StringBuilder sb, int value) {
    sb.append((char) (value < 10 ? ('0' + value) : ('A' + value - 10)));
  }
}
