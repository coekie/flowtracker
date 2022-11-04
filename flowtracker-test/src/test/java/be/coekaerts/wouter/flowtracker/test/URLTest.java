package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
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
      String descriptor = requireNonNull(InputStreamHook.getInputStreamTracker(in)).getDescriptor();
      assertTrue(descriptor.startsWith("FileInputStream for"));
      assertTrue(descriptor.endsWith(URLTest.class.getName().replace(".", "/") + ".class"));
    }
  }

  /** Test getting an input stream from a jar file */
  @Test public void jarURLConnection() throws IOException {
    URL url = requireNonNull(org.junit.Test.class.getResource("Test.class"));
    try (InputStream in = url.openStream()) {
      Tracker tracker = requireNonNull(InputStreamHook.getInputStreamTracker(in));
      assertTrue(tracker.getDescriptor().matches("Unzipped .*\\.jar file org/junit/Test.class"));
    }
  }
}
