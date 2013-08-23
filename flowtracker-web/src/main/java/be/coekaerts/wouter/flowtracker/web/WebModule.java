// Copyright 2013, Square, Inc.

package be.coekaerts.wouter.flowtracker.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class WebModule {
  public WebModule() throws Exception {
    Server server = new Server(8080);
    server.setHandler(new MyHandler());
    server.start();
  }

  private static class MyHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      response.setContentType("text/html;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);
      baseRequest.setHandled(true);
      response.getWriter().println("<h1>Hello World</h1>");
    }
  }
}
