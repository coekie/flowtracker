package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class URLTest {
  @Test public void openStream() throws IOException {
    URL url = URLTest.class.getResource("URLTest.class");
    InputStream in = url.openStream();
    String descriptor = TrackerRepository.getTracker(in).getDescriptor();
    assertTrue(descriptor.startsWith("InputStream from"));
    assertTrue(descriptor.endsWith(URLTest.class.getName().replace(".", "/") + ".class"));
    in.close();
  }
}
