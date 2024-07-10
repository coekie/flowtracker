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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.util.Config;
import jakarta.servlet.DispatcherType;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.gson.JsonGsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class WebModule {
  final Server server;

  public WebModule(Config config) throws Exception {
    if (config.getBoolean("webserver", true)) {
      server = startServer(config);
    } else {
      server = null;
    }
    String snapshotOnExitPath = config.get("snapshotOnExit");
    if (snapshotOnExitPath != null) {
      snapshotOnExit(snapshotOnExitPath, config);
    }
  }

  private static Server startServer(Config config) throws Exception {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(WebModule.class.getClassLoader());
    try {
      return doStartServer(config);
    } finally {
      Thread.currentThread().setContextClassLoader(ccl);
    }
  }

  private static Server doStartServer(Config config) throws Exception {
    // Setup server
    Server server = new Server(new TrackerSuspendingThreadPool());

    // make jetty scheduler threads daemon threads, so that we don't prevent the JVM from shutting
    // down.
    ScheduledExecutorScheduler scheduler =
        new ScheduledExecutorScheduler("flowtracker-jetty-scheduler", true);
    scheduler.start();
    server.addBean(scheduler);

    server.addConnector(createConnector(server, config));

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.setClassLoader(WebModule.class.getClassLoader());
    server.setHandler(context);

    ResourceConfig resourceConfig =
        new ResourceConfig(TrackerResource.class, TreeResource.class, SettingsResource.class,
            SnapshotResource.class, CodeResource.class)
            .property(ServletProperties.FILTER_FORWARD_ON_404, true)
            .property(ServerProperties.WADL_FEATURE_DISABLE, true)
            .property(ServerProperties.BV_FEATURE_DISABLE, true)
            .property(CommonProperties.PROVIDER_DEFAULT_DISABLE, "ALL")
            .register(JsonGsonFeature.class);
    ServletContainer servletContainer = new ServletContainer(resourceConfig);
    context.addFilter(new FilterHolder(servletContainer), "/*", EnumSet.of(DispatcherType.REQUEST));

    // Setup servlet for static resources; and because we need a servlet for the filter to work
    ServletHolder resourceServlet = new ServletHolder(DefaultServlet.class);
    @SuppressWarnings("ConstantConditions") // assume "static" directory exists
        String staticPath =
        WebModule.class.getClassLoader().getResource("static/").toExternalForm();
    resourceServlet.setInitParameter("resourceBase", staticPath);
    context.addServlet(resourceServlet, "/*");

    // Start
    server.start();

    return server;
  }

  private static Connector createConnector(Server server, Config config) {
    ServerConnector connector = new ServerConnector(server);
    connector.setHost("127.0.0.1");
    connector.setPort(Integer.parseInt(config.get("port", "8011")));
    return connector;
  }

  int getPort() {
    return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }

  private static void snapshotOnExit(String path, Config config) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      Context context = context();
      context.suspend();
      try (var out = new FileOutputStream(path)) {
        new Snapshot(TrackerTree.ROOT, config.getBoolean("snapshotOnExitMinimized", true))
            .write(out);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        context.unsuspend();
      }
    }));
  }

  /**
   * ThreadPool that creates threads that have tracking disabled, because we do not want to track
   * our own web server
   */
  private static class TrackerSuspendingThreadPool extends QueuedThreadPool {

    public TrackerSuspendingThreadPool() {
      // make these threads daemon threads
      setDaemon(true);
    }

    @Override public Thread newThread(final Runnable runnable) {
      return super.newThread(() -> {
        context().suspend();
        runnable.run();
      });
    }
  }
}
