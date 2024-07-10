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

import com.coekie.flowtracker.util.ShutdownSuspender;
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
    public boolean snapshot;
    public boolean suspendShutdown;
  }
}
