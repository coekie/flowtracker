// Copyright 2013, Square, Inc.

package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Test;

public class ClassTest {
  @Test public void getResourceAsStream() throws IOException {
    InputStream stream = URLTest.class.getResourceAsStream("URLTest.class");
    String descriptor = TrackerRepository.getTracker(stream).getDescriptor();
    Assert.assertTrue(descriptor.startsWith("InputStream from"));
    Assert.assertTrue(descriptor.endsWith(URLTest.class.getName().replace(".", "/") + ".class"));
  }
}
