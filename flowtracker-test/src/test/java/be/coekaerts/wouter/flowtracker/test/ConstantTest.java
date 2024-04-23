package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

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
    assertThat(point.tracker).isInstanceOf(ClassOriginTracker.class);
    TrackTestHelper.assertThatTrackerNode(point.tracker)
        .hasPath("Class", "be", "coekaerts", "wouter", "flowtracker", "test", "ConstantTest");
    assertThat(((ClassOriginTracker) point.tracker).getContent().charAt(point.index)).isEqualTo('a');
  }

  /** Test that the constantdynamic is doing its work, only creating one TrackerPoint instance */
  @Test
  public void testSamePointInstance() {
    char a = 'a';
    TrackerPoint point1 = FlowTester.getCharSourcePoint(a);
    TrackerPoint point2 = FlowTester.getCharSourcePoint(a);
    assertThat(point2).isSameInstanceAs(point1);
  }

  @Test
  public void testByteConstant() {
    byte b = 99;
    TrackerPoint point = FlowTester.getByteSourcePoint(b);
    assertThat(point.tracker).isInstanceOf(ClassOriginTracker.class);
    assertThat(((ClassOriginTracker) point.tracker).getContent().charAt(point.index)).isEqualTo(99);
  }

  @Test
  public void testSmallConstant() {
    byte b = 1; // gets compiled as ICONST_1 instruction
    TrackerPoint point = FlowTester.getByteSourcePoint(b);
    assertThat(point.tracker).isInstanceOf(ClassOriginTracker.class);
    assertThat(((ClassOriginTracker) point.tracker).getContent().charAt(point.index)).isEqualTo(1);
  }

  @Test
  public void testClassOriginTrackerContent() {
    char a = MyClass.myMethod(0, null);
    TrackerPoint point = FlowTester.getCharSourcePoint(a);
    ClassOriginTracker tracker = (ClassOriginTracker) point.tracker;
    assertThat(tracker.getContent().toString()).isEqualTo(
        "class be.coekaerts.wouter.flowtracker.test.ConstantTest$MyClass\n"
            + "char myMethod(int, java.lang.String):\n"
            + "  c\n");
  }

  static class MyClass {
    // method signature with parameters to test how it's represented as a String
    @SuppressWarnings("unused")
    static char myMethod(int i, String s) {
      return 'c';
    }
  }
}
