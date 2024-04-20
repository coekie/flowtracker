package be.coekaerts.wouter.flowtracker.web;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

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
    assertThat(requireNonNull(webModule.server).isStarted()).isTrue();

    // entry point
    String index = get("/");
    assertThat(index).contains("<title>Flowtracker</title>");

    // static files
    assertThat(get("/folder.svg")).contains("<svg");

    // compiled js
    String jsPath = get(index.substring(index.indexOf("/assets/"), index.indexOf(".js") + 3));
    assertThat(jsPath).contains("svelte");

    // rest endpoints
    assertThat(get("/tree/all")).contains("root");
  }

  @After
  public void after() throws Exception {
    requireNonNull(webModule.server).stop();
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
