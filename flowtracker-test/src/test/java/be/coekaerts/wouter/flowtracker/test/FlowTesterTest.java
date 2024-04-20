package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FlowTesterTest {
  private final FlowTester ft = new FlowTester();
  private final FlowTester ft2 = new FlowTester();

  @Test
  public void testCreateAndAssertTrackedValue_char() {
    char a = ft.createSourceChar('a');
    FlowTester.assertTrackedValue(a, 'a', ft.theSource(), ft.theSourceIndex());
  }

  @Test
  public void testCreateAndAssertTrackedValue_byte() {
    byte a = ft.createSourceByte((byte) 'a');
    FlowTester.assertTrackedValue(a, (byte) 'a', ft.theSource(), ft.theSourceIndex());
  }

  @Test
  public void testCreateAndAssertIsTheTrackedValue_char() {
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
  public void testCreateAndAssertIsTheTrackedValue_byte() {
    byte a = ft.createSourceByte((byte) 'a');
    byte b = ft2.createSourceByte((byte) 'b');
    ft.assertIsTheTrackedValue(a);
    ft2.assertIsTheTrackedValue(b);

    try {
      ft.assertIsTheTrackedValue(b);
      throw new RuntimeException("failed");
    } catch (AssertionError expected) {
    }
  }

  @Test
  public void testGetTracker_char() {
    char a = ft.createSourceChar('a');
    assertThat(FlowTester.getCharSourceTracker(a)).isEqualTo(ft.theSource());
    assertThat(FlowTester.getCharSourcePoint(a)).isEqualTo(ft.theSourcePoint());
  }

  @Test
  public void testGetTracker_byte() {
    byte a = ft.createSourceByte((byte) 'a');
    assertThat(FlowTester.getByteSourceTracker(a)).isEqualTo(ft.theSource());
    assertThat(FlowTester.getByteSourcePoint(a)).isEqualTo(ft.theSourcePoint());
  }
}
