package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class WebModule {
  public WebModule() throws Exception {
    // Setup server and servlet context
    Server server = new Server(new TrackerSuspendingThreadPool());
    server.addConnector(createConnector(server));

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.setClassLoader(WebModule.class.getClassLoader());
    server.setHandler(context);

    ResourceConfig resourceConfig =
        new ResourceConfig(TrackerResource.class, SettingsResource.class)
            .property(ServletProperties.FILTER_FORWARD_ON_404, true)
            .register(JacksonFeature.class);
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
  }

  private static Connector createConnector(Server server) {
    ServerConnector connector = new ServerConnector(server);
    connector.setHost("127.0.0.1");
    connector.setPort(8080);
    return connector;
  }

  /**
   * ThreadPool that creates threads that have tracking disabled, because we do not want to track
   * our own web server
   */
  private static class TrackerSuspendingThreadPool extends QueuedThreadPool {
    @Override public Thread newThread(final Runnable runnable) {
      return super.newThread(new Runnable() {
        @Override public void run() {
          Trackers.suspendOnCurrentThread();
          runnable.run();
        }
      });
    }
  }
}
