package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
    ShutdownSuspender.setSuspendShutdown(settings.isSuspendShutdown());
  }

  public static class Settings {
    private boolean suspendShutdown;

    public boolean isSuspendShutdown() {
      return suspendShutdown;
    }

    public void setSuspendShutdown(boolean suspendShutdown) {
      this.suspendShutdown = suspendShutdown;
    }
  }
}
