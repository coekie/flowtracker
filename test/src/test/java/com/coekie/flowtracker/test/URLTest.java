package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.hook.InputStreamHook;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.Test;

public class URLTest {
  /**
   * Test getting an input stream from a file in the filesystem. We use our own class file for
   * convenience.
   */
  @Test public void fileURLConnection() throws IOException {
    URL url = requireNonNull(URLTest.class.getResource("URLTest.class"));
    try (InputStream in = url.openStream()) {
      assertThatTrackerNode(InputStreamHook.getInputStreamTracker(in))
          .hasPathStartingWith("Files")
          .hasPathEndingWith("URLTest.class");
    }
  }

  /** Test getting an input stream from a jar file */
  @Test public void jarURLConnection() throws IOException {
    URL url = requireNonNull(org.junit.Test.class.getResource("Test.class"));
    try (InputStream in = url.openStream()) {
      assertThatTrackerNode(InputStreamHook.getInputStreamTracker(in))
          .hasPathStartingWith("Files")
          .hasPathEndingWith("org", "junit", "Test.class");
    }
  }
}
