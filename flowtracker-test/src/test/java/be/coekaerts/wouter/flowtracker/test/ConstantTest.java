package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import org.junit.Test;

public class ConstantTest {
  @Test
  public void testCharConstant() {
    Tracker tracker = FlowTester.getCharSourceTracker('a');
    assertEquals(ConstantTest.class.getName().replace('.', '/'), tracker.getDescriptor());
  }

  @Test
  public void testByteConstant() {
    byte b = 99;
    Tracker tracker = FlowTester.getByteSourceTracker(b);
    assertEquals(ConstantTest.class.getName().replace('.', '/'), tracker.getDescriptor());
  }

  @Test
  public void testSmallConstant() {
    byte b = 1; // gets compiled as ICONST_1 instruction
    Tracker tracker = FlowTester.getByteSourceTracker(b);
    assertEquals(ConstantTest.class.getName().replace('.', '/'), tracker.getDescriptor());
  }
}
