// Copyright 2013, Square, Inc.

package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Test for the assembly, and for {@link be.coekaerts.wouter.flowtracker.agent.FlowTrackAgent}'s
 * handling of it
 */
public class AssemblyTest {

  @Test
  public void testSetupSanity() {
    // The point of this test class is to ensure the assembly works.
    // To properly test that, we must not have anything else than the assembly (and junit) on the
    // classpath.
    for (String classpathPart : System.getProperty("java.class.path").split(":")) {
      if (!classpathPart.contains("assembly") && !classpathPart.contains("junit")) {
        fail("Unexpected classpath in test: " + classpathPart);
      }
    }
  }

  @Test
  public void simpleTrackingTest() {
    char[] a = new char['a'];
    Tracker aTracker = TrackerRepository.createFixedOriginTracker(a, a.length);
    char[] copy = Arrays.copyOf(a, a.length);
    assertSame(aTracker, TrackerRepository.getTracker(copy).getEntryAt(0).getValue().getTracker());
  }
}
