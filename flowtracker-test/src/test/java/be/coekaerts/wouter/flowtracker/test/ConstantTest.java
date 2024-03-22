package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;
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
        .hasDescriptor(ConstantTest.class.getName().replace('.', '/'))
        .hasNode("Class", "be", "coekaerts", "wouter", "flowtracker", "test", "ConstantTest");
    assertEquals('a', ((ClassOriginTracker) point.tracker).getContent().charAt(point.index));
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
}
