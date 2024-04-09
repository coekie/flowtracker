package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/** Serves the {@link Snapshot} */
@Path("/snapshot")
public class SnapshotResource {
  @GET
  public Response get() {
    StreamingOutput streamingOutput = output -> new Snapshot(TrackerTree.ROOT).write(output);
    return Response.ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"flowtracker-snapshot.zip\"")
        .build();
  }
}
