package demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Starts a jdk {@link HttpServer} and sends a request to it using {@link HttpClient}.
 */
public class JdkHttpDemo {
  public static void main(String... args) throws Exception {
    HttpServer server = startServer();
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI("http://localhost:" + server.getAddress().getPort() + "/test"))
          .build();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      System.out.println("Response body: " + response.body());
    } finally {
      server.stop(0);
    }
  }

  static HttpServer startServer() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/test", new MyHandler());
    server.start();
    return server;
  }

  static class MyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String response = "This is the response";
      t.sendResponseHeaders(200, response.length());
      t.getResponseBody().write(response.getBytes());
      t.close();
    }
  }
}
