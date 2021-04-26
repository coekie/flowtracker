package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FlowTesterTest {
  private final FlowTester ft = new FlowTester();
  private final FlowTester ft2 = new FlowTester();

  @Test
  public void testCreateAndAssertTrackedValue() {
    char a = ft.createSourceChar('a');
    FlowTester.assertTrackedValue(a, ft.theSource(), ft.theSourceIndex());
  }

  @Test
  public void testCreateAndAssertIsTheTrackedValue() {
    char a = ft.createSourceChar('a');
    char b = ft2.createSourceChar('b');
    ft.assertIsTheTrackedValue(a);
    ft2.assertIsTheTrackedValue(b);

    try {
      ft.assertIsTheTrackedValue(b);
      throw new RuntimeException("failed");
    } catch (AssertionError expected) {
    }
  }

  @Test
  public void testGetTracker() {
    char a = ft.createSourceChar('a');
    assertEquals(ft.theSource(), FlowTester.getCharSourceTracker(a));
    assertEquals(ft.theSourceIndex(), FlowTester.getCharSourceIndex(a));
  }
}
