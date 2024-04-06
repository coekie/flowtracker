package be.coekaerts.wouter.flowtracker.web;

import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.util.Config;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

/**
 * Test that the webserver starts up and serves the expected endpoints
 */
public class WebModuleTest {
  private WebModule webModule;

  @Test
  public void test() throws Exception {
    webModule = new WebModule(Config.forTesting(Map.of("port", "0")));
    assertTrue(webModule.server.isStarted());

    // entry point
    String index = get("/");
    assertContains("<title>Flowtracker</title>", index);

    // static files
    assertContains("<svg", get("/folder.svg"));

    // compiled js
    String jsPath = get(index.substring(index.indexOf("/assets/"), index.indexOf(".js") + 3));
    assertContains("svelte", jsPath);

    // rest endpoints
    assertContains("<root>", get("/tree"));
  }

  @After
  public void after() throws Exception {
    webModule.server.stop();
  }

  private void assertContains(String expected, String str) {
    if (!str.contains(expected)) {
      throw new AssertionError("Count not find " + expected + " in " + str);
    }
  }

  private String get(String path) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + webModule.getPort() + path))
        .build();

    HttpResponse<String> response =
        client.send(request, BodyHandlers.ofString());

    return response.body();
  }
}
