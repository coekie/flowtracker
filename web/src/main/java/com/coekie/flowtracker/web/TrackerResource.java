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
import com.coekie.flowtracker.tracker.TwinSynchronization;
import com.coekie.flowtracker.tracker.TwinSynchronization.TwinMarker;
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
import java.util.function.Consumer;

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
    ResponseBuilder builder = new ResponseBuilder(tracker);
    if (tracker instanceof DefaultTracker) {
      Simplifier.simplifySourceTo(tracker, (index, length, sourceTracker, sourceIndex, growth) -> {
        if (sourceTracker != null) {
          builder.addPart(index, index + length,
              builder.part(sourceTracker, sourceIndex, growth.targetToSource(length)));
        }
      });
    }
    return new TrackerDetailResponse(builder);
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

    ResponseBuilder builder = new ResponseBuilder(tracker);

    // record at which indexes in tracker that changes happen to which parts correspond to it.
    // in other words, this iterates over the content of `target`, and builds an index of how that
    // maps to indexes in the content of `tracker`
    Simplifier.simplifySourceTo(target, new WritableTracker() {
      @Override
      public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
          Growth growth) {
        int sourceLength = growth.targetToSource(length);
        if (sourceTracker == tracker) {
          TrackerPartResponse part = builder.part(target, index, length);
          if (includeParts) {
            builder.addPart(sourceIndex, sourceIndex + sourceLength, part);
          } else {
            // split up in regions so there's a region boundary at start and end of each part
            builder.addChange(sourceIndex, state -> {});
            builder.addChange(sourceIndex + sourceLength, state -> {});
          }
        } else if (sourceTracker != null && sourceTracker.getEntryCount() > 0) {
          // recurse to the source of the source
          sourceTracker.pushSourceTo(sourceIndex, this, index, sourceLength, growth);
        }
      }
    });

    return new TrackerDetailResponse(builder);
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

    /** @see Tracker#twin() */
    public final TrackerResponse twin;

    private TrackerDetailResponse(ResponseBuilder builder) {
      this.path = path(builder.tracker);
      this.creationStackTrace = creationStackTraceToString(builder.tracker);
      this.regions = builder.buildRegions();
      this.linkedTrackers = builder.linkedTrackers;
      this.hasSource = builder.hasSource;
      this.twin = builder.tracker.twin() == null ? null : new TrackerResponse(
          builder.tracker.twin());
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

    /**
     * Content from the twin ({@link Tracker#twin()}) that appeared just before this region.
     * For example, if this region contains an HTTP response, then the twinContent could be the HTTP
     * request that caused it.
     */
    public final String twinContent;

    Region(Tracker tracker, int offset, int length, List<TrackerPartResponse> parts, int line,
        String twinContent) {
      this.offset = offset;
      this.length = length;
      this.content = getContentAsString(tracker, offset, offset + length);
      this.parts = parts;
      this.line = line == -1 ? null : line;
      this.twinContent = twinContent;
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
   * Builds a {@link TrackerDetailResponse}.
   * Makes sure every tracker referenced from a {@link TrackerPartResponse} is in the
   * {@link #linkedTrackers} map
   */
  static class ResponseBuilder {
    private final Tracker tracker;

    private final Map<Long, TrackerResponse> linkedTrackers = new HashMap<>();

    /**
     * Records at which indexes in tracker that changes happen to {@link State}, e.g. which parts
     * correspond to it. A change means the start or end of an associated part.
     * The Consumer in this map mutates {@link State} with the relevant change.
     * This allows building an ~index of where the region boundaries are out of order, and then
     * going over it in order to build the regions.
     */
    private final TreeMap<Integer, List<Consumer<State>>> changePoints = new TreeMap<>();

    private final TwinSynchronization twinSync;
    private boolean hasSource;

    ResponseBuilder(Tracker tracker) {
      this.tracker = tracker;
      changePoints.put(0, new ArrayList<>());
      twinSync = getTwinSync(tracker);
      addSourceAndLineNumbers();
      addTwinContent();
    }

    /** Create a TrackerPartResponse, and make sure it's included in {@link #linkedTrackers} */
    TrackerPartResponse part(Tracker tracker, int offset, int length) {
      if (!linkedTrackers.containsKey(tracker.getTrackerId())) {
        linkedTrackers.put(tracker.getTrackerId(), new TrackerResponse(tracker));
      }
      return new TrackerPartResponse(tracker, offset, length);
    }

    /**
     * Mark a range in the response to link to the given part. This will cause the response to be
     * split into more {@link Region}s if necessary.
     */
    void addPart(int begin, int end, TrackerPartResponse part) {
      addChange(begin, state -> state.parts.add(part));
      addChange(end, state -> state.parts.remove(part));
    }

    /**
     * Register a change in the {@link State} to happen at the given index. The Consumer will be
     * executed (so it can update the state) when building the final response.
     */
    void addChange(int index, Consumer<State> change) {
      changePoints.computeIfAbsent(index, i -> new ArrayList<>()).add(change);
    }

    /**
     * For ClassOriginTracker, mark it has having source code, and mark positions where the line
     * number changes
     */
    void addSourceAndLineNumbers() {
      if (tracker instanceof ClassOriginTracker) {
        hasSource = true;
        ((ClassOriginTracker) tracker).pushLineNumbers((start, end, line) -> {
          addChange(start, state -> state.lineNumber = line);
          addChange(end, state -> state.lineNumber = -1);
        });
      }
    }

    private static TwinSynchronization getTwinSync(Tracker tracker) {
      if (tracker instanceof OriginTracker) {
        return ((OriginTracker) tracker).twinSync();
      } else if (tracker.twin() instanceof OriginTracker) {
        return ((OriginTracker) tracker.twin()).twinSync();
      } else {
        return null;
      }
    }

    /**
     * Add changePoints for setting {@link State#pendingTwinContent}.
     */
    void addTwinContent() {
      if (twinSync == null) {
        return;
      }
      for (TwinMarker marker : twinSync.markers) {
        if (marker.to == tracker) {
          addChange(marker.toIndex, state -> {
            state.pendingTwinContent =
                getContentAsString(marker.from(), state.prevTwinIndex, marker.fromIndex);
            state.prevTwinIndex = marker.fromIndex;
          });
        }
      }
    }

    List<Region> buildRegions() {
      State state = new State();
      List<Region> regions = new ArrayList<>();
      // iterate of the content of `tracker`, building up regions.
      for (int i : changePoints.keySet()) {
        // update activeParts to match the parts active at i
        for (Consumer<State> change : changePoints.get(i)) {
          change.accept(state);
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
            new ArrayList<>(state.parts),
            state.lineNumber, state.pendingTwinContent));
        state.pendingTwinContent = null;
      }

      // if there's still content from the twin after our last region, then add an extra empty
      // region at the end
      if (twinSync != null) {
        Tracker other = twinSync.other(tracker);
        if (other.getLength() > state.prevTwinIndex) {
          regions.add(new Region(tracker, getContentLength(tracker), 0, List.of(), -1,
              getContentAsString(other, state.prevTwinIndex, other.getLength())));
        }
      }

      return regions;
    }

    /** State that is updated while we're iterating over the content when building the response */
    private static class State {
      /** Parts that the current region should link to */
      List<TrackerPartResponse> parts = new ArrayList<>();

      /** Current line number */
      int lineNumber = -1;

      /** Content from the twin that still needs to be included in {@link Region#twinContent} */
      String pendingTwinContent;

      /** Index up to where we've already written {@link #pendingTwinContent} */
      int prevTwinIndex;
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
