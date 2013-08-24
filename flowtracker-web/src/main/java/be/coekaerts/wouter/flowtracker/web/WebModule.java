package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class WebModule {
  public WebModule() throws Exception {
    // Setup server and servlet context
    Server server = new Server(new InetSocketAddress("127.0.0.1", 8080));
    server.setThreadPool(new TrackerSuspendingThreadPool());
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.setClassLoader(WebModule.class.getClassLoader());
    server.setHandler(context);

    // Setup Wicket filter
    FilterHolder filterHolder = new FilterHolder(WicketFilter.class);
    filterHolder.setInitParameter("applicationClassName", FlowtrackerApplication.class.getName());
    filterHolder.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
    context.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

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

  /**
   * ThreadPool that creates threads that have tracking disabled, because we do not want to track
   * our own web server
   */
  private static class TrackerSuspendingThreadPool extends QueuedThreadPool {
    @Override protected Thread newThread(final Runnable runnable) {
      return super.newThread(new Runnable() {
        @Override public void run() {
          Trackers.suspendOnCurrentThread();
          runnable.run();
        }
      });
    }
  }

}
