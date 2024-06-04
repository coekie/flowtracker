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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

/** Serves source code for classes referenced by {@link ClassOriginTracker}. */
@Path("/src")
public class SourceResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public SourceResponse get(@PathParam("id") long id) throws IOException {
    ClassOriginTracker tracker = (ClassOriginTracker) InterestRepository.getContentTracker(id);

    // TODO use tracker.sourceFile to find read source

    return getWithAsm(tracker);
  }

  /** Use ASM to dump the bytecode */
  private SourceResponse getWithAsm(ClassOriginTracker tracker) throws IOException {
    try (InputStream is = getAsStream(tracker.loader, tracker.className + ".class")) {
      if (is == null) {
        return null;
      }

      StringWriter sw = new StringWriter();
      // TODO enhance Textifier to track line numbers
      TraceClassVisitor traceClassVisitor =
          new TraceClassVisitor(null, new Textifier(), new PrintWriter(sw));
      ClassReader reader = new ClassReader(is);
      reader.accept(traceClassVisitor, ClassReader.SKIP_FRAMES);

      return new SourceResponse(List.of(new Line(null, sw.toString())));
    }
  }

  private static InputStream getAsStream(ClassLoader loader, String path) {
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

  public static class Line {
    public final Integer line;
    public final String content;

    public Line(Integer line, String content) {
      this.line = line;
      this.content = content;
    }
  }
}
