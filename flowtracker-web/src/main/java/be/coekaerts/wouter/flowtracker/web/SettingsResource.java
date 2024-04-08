package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/settings")
public class SettingsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Settings get() {
    Settings settings = new Settings();
    settings.suspendShutdown = ShutdownSuspender.isSuspendShutdown();
    return settings;
  }

  @POST
  public void set(Settings settings) {
    ShutdownSuspender.setSuspendShutdown(settings.suspendShutdown);
  }

  public static class Settings {
    public boolean suspendShutdown;
  }
}
