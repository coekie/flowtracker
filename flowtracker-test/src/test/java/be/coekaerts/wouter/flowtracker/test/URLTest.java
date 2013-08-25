// Copyright 2013, Square, Inc.

package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

public class URLTest {
  @Test public void openStream() throws IOException {
    URL url = URLTest.class.getResource("URLTest.class");
    InputStream in = url.openStream();
    String descriptor = TrackerRepository.getTracker(in).getDescriptor();
    Assert.assertTrue(descriptor.startsWith("InputStream from"));
    Assert.assertTrue(descriptor.endsWith(URLTest.class.getName().replace(".", "/") + ".class"));
    in.close();
  }
}
