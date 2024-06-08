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

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.web.TrackerResource.TrackerPartResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Serves source code for classes referenced by {@link ClassOriginTracker}. */
@Path("/code")
public class SourceResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public SourceResponse get(@PathParam("id") long id) throws IOException {
    return getAll(List.of(id)).get(id);
  }

  /**
   * Get the source code for a collection of trackers.
   * We do this in batch here when taking a snapshot, because that's faster than handling them one
   * by one in {@link VineflowerSourceGenerator}.
   */
  Map<Long, SourceResponse> getAll(Collection<Long> ids) throws IOException {
    // first try finding the actual source file
    List<Long> remaining = new ArrayList<>();
    Map<Long, SourceResponse> result = new HashMap<>();
    for (Long id : ids) {
      SourceResponse found = SourceSourceGenerator.getSource(
          (ClassOriginTracker) InterestRepository.getContentTracker(id));
      if (found != null) {
        result.put(id, found);
      } else {
        remaining.add(id);
      }
    }

    // otherwise decompile with vineflower
    result.putAll(getAllWithVineflower(remaining));

    // for the ones that we couldn't get from source or vineflower, fallback to AsmSourceGenerator
    for (Long id : ids) {
      if (!result.containsKey(id)) {
        ClassOriginTracker tracker = (ClassOriginTracker) InterestRepository.getContentTracker(id);
        result.put(id, AsmSourceGenerator.getSource(tracker));
      }
    }
    return result;
  }

  private Map<Long, SourceResponse> getAllWithVineflower(Collection<Long> ids) {
    Map<ClassLoader, List<ClassOriginTracker>> trackersByClassLoader = new HashMap<>();
    for (long trackerId : ids) {
      Tracker t = InterestRepository.getContentTracker(trackerId);
      if (t instanceof ClassOriginTracker) {
        ClassOriginTracker tracker = (ClassOriginTracker) t;
        trackersByClassLoader.computeIfAbsent(tracker.loader, l -> new ArrayList<>())
            .add(tracker);
      }
    }

    Map<Long, SourceResponse> result = new HashMap<>();
    for (List<ClassOriginTracker> trackers : trackersByClassLoader.values()) {
      result.putAll(VineflowerSourceGenerator.getSource(trackers));
    }
    return result;
  }

  static InputStream getAsStream(ClassLoader loader, String path) {
    if (loader == null) { // class from the bootstrap classloader
      return String.class.getResourceAsStream('/' + path);
    } else {
      return loader.getResourceAsStream(path);
    }
  }

  static Map<Integer, List<TrackerPartResponse>> lineToPartMapping(
      ClassOriginTracker tracker) {
    Map<Integer, List<TrackerPartResponse>> result = new HashMap<>();
    tracker.pushLineNumbers((start, end, line)
        -> result.computeIfAbsent(line, l -> new ArrayList<>())
        .add(new TrackerPartResponse(tracker, start, end - start)));
    return result;
  }

  public static class SourceResponse {
    public final List<Line> lines;

    SourceResponse(List<Line> lines) {
      this.lines = lines;
    }
  }

  /** Block of source code (or bytecode representation) that maps to one line in the source code */
  public static class Line {
    /**
     * The line number, or null if this part (as far as we know) does not correspond to a line in
     * the source
     */
    public final Integer line;

    /** Content of this line. This can consist of multiple lines of text (that all correspond to one
     * line in the original source code). Includes a newline at the end.
     */
    public final String content;

    /** Parts of the tracker that refer to this line */
    public final List<TrackerPartResponse> parts;

    public Line(Integer line, String content, List<TrackerPartResponse> parts) {
      this.line = line;
      this.content = content;
      this.parts = parts;
    }
  }
}
