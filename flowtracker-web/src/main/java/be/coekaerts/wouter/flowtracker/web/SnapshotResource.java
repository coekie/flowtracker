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
