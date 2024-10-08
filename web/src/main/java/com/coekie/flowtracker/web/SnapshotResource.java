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

import com.coekie.flowtracker.tracker.TrackerTree;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/** Serves the {@link Snapshot} */
@Path("/snapshot")
public class SnapshotResource {
  @GET
  @Path("full")
  public Response full() {
    return get(false);
  }

  @GET
  @Path("minimized")
  public Response minimized() {
    return get(true);
  }

  private Response get(boolean minimized) {
    StreamingOutput streamingOutput = output ->
        new Snapshot(TrackerTree.ROOT, minimized).write(output);
    return Response.ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"flowtracker-snapshot.zip\"")
        .build();
  }
}
