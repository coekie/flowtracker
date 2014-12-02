package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClassTest {
  @Test public void getResourceAsStream() throws IOException {
    InputStream stream = URLTest.class.getResourceAsStream("URLTest.class");
    String descriptor = TrackerRepository.getTracker(stream).getDescriptor();
    assertTrue(descriptor.startsWith("InputStream from"));
    assertTrue(descriptor.endsWith(URLTest.class.getName().replace(".", "/") + ".class"));
  }
}
