package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
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
        .hasPath("Class", "com", "coekie", "flowtracker", "test", "ConstantTest");
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
    assertThat(((ClassOriginTracker) point.tracker).getContent()
        .subSequence(point.index, point.index + point.length)).isEqualTo("0x1 (1)");
  }

  @Test
  public void testNonPrintableInt() {
    int i = 999;
    TrackerPoint point = FlowTester.getIntSourcePoint(i);
    assertThat(point.tracker).isInstanceOf(ClassOriginTracker.class);
    assertThat(((ClassOriginTracker) point.tracker).getContent()
        .subSequence(point.index, point.index + point.length)).isEqualTo("0x3e7 (999)");
  }

  @Test
  public void testLargeNonPrintableInt() {
    int i = 999999;
    TrackerPoint point = FlowTester.getIntSourcePoint(i);
    assertThat(point.tracker).isInstanceOf(ClassOriginTracker.class);
    assertThat(((ClassOriginTracker) point.tracker).getContent()
        .subSequence(point.index, point.index + point.length)).isEqualTo("0xf423f (999999)");
  }

  @Test
  public void testClassOriginTrackerContent() {
    char a = MyClass.myMethod(0, null);
    TrackerPoint point = FlowTester.getCharSourcePoint(a);
    ClassOriginTracker tracker = (ClassOriginTracker) point.tracker;
    assertThat(tracker.getContent().toString()).isEqualTo(
        "class com.coekie.flowtracker.test.ConstantTest$MyClass\n"
            + "char myMethod(int, java.lang.String):\n"
            + "  (line 81) c\n");
  }

  static class MyClass {
    // method signature with parameters to test how it's represented as a String
    @SuppressWarnings("unused")
    static char myMethod(int i, String s) {
      return 'c';
    }
  }
}
