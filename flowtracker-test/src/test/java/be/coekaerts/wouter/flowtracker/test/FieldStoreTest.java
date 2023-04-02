package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.FieldRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import org.junit.Test;

public class FieldStoreTest {
  @SuppressWarnings("FieldCanBeLocal")
  private char c;

  @Test
  public void test() {
    FlowTester tester = new FlowTester();
    c = tester.createSourceChar('a');
    String fieldId =
        FieldRepository.fieldId("be/coekaerts/wouter/flowtracker/test/FieldStoreTest", "c");
    TrackerPoint point = requireNonNull(FieldRepository.getPoint(this, fieldId));
    assertEquals(tester.theSource(), point.tracker);
    assertEquals(tester.theSourceIndex(), point.index);
    assertEquals('a', c);
  }
}
