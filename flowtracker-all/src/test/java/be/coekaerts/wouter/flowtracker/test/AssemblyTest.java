package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static org.junit.Assert.fail;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.util.Arrays;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

/**
 * Test for the assembly, and for {@link be.coekaerts.wouter.flowtracker.agent.FlowTrackAgent}'s
 * handling of it
 */
public class AssemblyTest {

  @Test public void setupSanity() {
    // The point of this test class is to ensure the assembly works.
    // To properly test that, we must not have anything else than flowtracker-all and junit on the
    // classpath.
    for (String classpathPart : System.getProperty("java.class.path").split(":")) {
      if (!classpathPart.contains("flowtracker-all") && !classpathPart.contains("junit")
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
    Tracker aTracker = TrackerRepository.createFixedOriginTracker(a, a.length);
    char[] copy = Arrays.copyOf(a, a.length);
    assertThatTrackerOf(copy).matches(snapshot().part(aTracker));
  }
}
