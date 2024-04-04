package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import org.junit.Test;

/**
 * Test tracking of primitive constants
 */
public class ConstantTest {
  @Test
  public void testCharConstant() {
    TrackerPoint point = FlowTester.getCharSourcePoint('a');
    assertTrue(point.tracker instanceof ClassOriginTracker);
    TrackTestHelper.assertThatTracker(point.tracker)
        .hasNode("Class", "be", "coekaerts", "wouter", "flowtracker", "test", "ConstantTest");
    assertEquals('a', ((ClassOriginTracker) point.tracker).getContent().charAt(point.index));
  }

  /** Test that the constantdynamic is doing its work, only creating one TrackerPoint instance */
  @Test
  public void testSamePointInstance() {
    char a = 'a';
    TrackerPoint point1 = FlowTester.getCharSourcePoint(a);
    TrackerPoint point2 = FlowTester.getCharSourcePoint(a);
    assertSame(point1, point2);
  }

  @Test
  public void testByteConstant() {
    byte b = 99;
    TrackerPoint point = FlowTester.getByteSourcePoint(b);
    assertTrue(point.tracker instanceof ClassOriginTracker);
    assertEquals(99, ((ClassOriginTracker) point.tracker).getContent().charAt(point.index));
  }

  @Test
  public void testSmallConstant() {
    byte b = 1; // gets compiled as ICONST_1 instruction
    TrackerPoint point = FlowTester.getByteSourcePoint(b);
    assertTrue(point.tracker instanceof ClassOriginTracker);
    assertEquals(1, ((ClassOriginTracker) point.tracker).getContent().charAt(point.index));
  }

  @Test
  public void testClassOriginTrackerContent() {
    char a = MyClass.myMethod(0, null);
    TrackerPoint point = FlowTester.getCharSourcePoint(a);
    ClassOriginTracker tracker = (ClassOriginTracker) point.tracker;
    assertEquals("class be/coekaerts/wouter/flowtracker/test/ConstantTest$MyClass\n"
            + "char myMethod(int, java.lang.String):\n"
            + "  c\n",
        tracker.getContent().toString());
  }

  static class MyClass {
    // method signature with parameters to test how it's represented as a String
    @SuppressWarnings("unused")
    static char myMethod(int i, String s) {
      return 'c';
    }
  }
}
