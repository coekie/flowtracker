package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class URLTest {
  /**
   * Test getting an input stream from a file in the filesystem. We use our own class file for
   * convenience.
   */
  @Test public void fileURLConnection() throws IOException {
    URL url = URLTest.class.getResource("URLTest.class");
    InputStream in = url.openStream();
    String descriptor = TrackerRepository.getTracker(in).getDescriptor();
    assertTrue(descriptor.startsWith("InputStream from file:"));
    assertTrue(descriptor.endsWith(URLTest.class.getName().replace(".", "/") + ".class"));
    in.close();
  }

  /** Test getting an input stream from a jar file */
  @Test public void jarURLConnection() throws IOException {
    URL url = String.class.getResource("String.class");
    InputStream in = url.openStream();
    String descriptor = TrackerRepository.getTracker(in).getDescriptor();
    assertTrue(descriptor.startsWith("InputStream from jar:file:"));
    assertTrue(descriptor.endsWith(".jar!/java/lang/String.class"));
    System.out.println(TrackerRepository.getTracker(in).getDescriptor());
  }
}
