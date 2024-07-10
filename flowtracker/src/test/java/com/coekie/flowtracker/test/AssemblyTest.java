package com.coekie.flowtracker.test;

import static org.junit.Assert.fail;

import com.coekie.flowtracker.agent.FlowTrackerAgent;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.util.Arrays;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

/**
 * Test for the assembly, and for {@link FlowTrackerAgent}'s handling of it
 */
public class AssemblyTest {

  @Test public void setupSanity() {
    // The point of this test class is to ensure the assembly works.
    // To properly test that, we must not have anything else than flowtracker.jar and test
    // dependencies on the classpath.
    for (String classpathPart : System.getProperty("java.class.path").split(":")) {
      if (!classpathPart.contains("flowtracker.jar")
          && !classpathPart.contains("flowtracker/flowtracker/target/classes")
          && !classpathPart.contains("flowtracker/flowtracker/target/test-classes")
          && !classpathPart.contains("junit")
          && !classpathPart.contains("hamcrest-core")) {
        fail("Unexpected classpath in test: " + classpathPart);
      }
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test(expected = NoClassDefFoundError.class)
  public void dependenciesNotInAppClasspath() {
    ClassReader.class.getMethods();
  }

  @Test public void simpleTrackingTest() {
    char[] a = new char['a'];
    Tracker aTracker = TrackerRepository.createFakeOriginTracker(a, a.length);
    char[] copy = Arrays.copyOf(a, a.length);
    TrackerSnapshot.assertThatTrackerOf(copy).matches(TrackerSnapshot.snapshot().part(aTracker));
  }
}
