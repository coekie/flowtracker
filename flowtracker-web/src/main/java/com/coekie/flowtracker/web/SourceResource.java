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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Serves source code for classes referenced by {@link ClassOriginTracker}. */
@Path("/code")
public class SourceResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public SourceResponse get(@PathParam("id") long id) throws IOException {
    ClassOriginTracker tracker = (ClassOriginTracker) InterestRepository.getContentTracker(id);
    return AsmSourceGenerator.getSource(tracker);
  }

  static InputStream getAsStream(ClassLoader loader, String path) {
    if (loader == null) { // class from the bootstrap classloader
      return String.class.getResourceAsStream('/' + path);
    } else {
      return loader.getResourceAsStream(path);
    }
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

    public Line(Integer line, String content) {
      this.line = line;
      this.content = content;
    }
  }
}
