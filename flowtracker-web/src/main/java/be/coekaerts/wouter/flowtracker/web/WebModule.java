package be.coekaerts.wouter.flowtracker.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class WebModule {
  public WebModule() throws Exception {
    // Setup server and servlet context
    Server server = new Server(new InetSocketAddress("127.0.0.1", 8080));
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

  public static class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
        ServletException, IOException {
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println("<h1>Hello SimpleServlet</h1>");
    }
  }

}
