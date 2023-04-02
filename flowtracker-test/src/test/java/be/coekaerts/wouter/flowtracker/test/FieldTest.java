package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.FieldRepository;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import org.junit.Test;

public class FieldTest {
  @SuppressWarnings("FieldCanBeLocal")
  private char c;

  @Test
  public void testFieldStore() {
    FlowTester tester = new FlowTester();
    c = tester.createSourceChar('a');
    String fieldId =
        FieldRepository.fieldId("be/coekaerts/wouter/flowtracker/test/FieldTest", "c");
    TrackerPoint point = requireNonNull(FieldRepository.getPoint(this, fieldId));
    assertEquals(tester.theSource(), point.tracker);
    assertEquals(tester.theSourceIndex(), point.index);
    assertEquals('a', c);
  }

  @Test
  public void testFieldValue() {
    TrackerPoint point = TrackerPoint.of(new FixedOriginTracker(2), 42);
    String fieldId =
        FieldRepository.fieldId("be/coekaerts/wouter/flowtracker/test/FieldTest", "c");
    FieldRepository.setPoint(this, fieldId, point);

    FlowTester.assertTrackedValue(c, (char) 0, point.tracker, 42);
  }

  @Test
  public void testFieldStoreAndValue() {
    FlowTester tester = new FlowTester();
    c = tester.createSourceChar('a');
    tester.assertIsTheTrackedValue(c);
  }
}
