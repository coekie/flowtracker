package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.FieldRepository;
import com.coekie.flowtracker.tracker.FixedOriginTracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import org.junit.Test;

/**
 * Note: for tests on java.lang.reflect.Field, see {@link ReflectionTest}.
 */
public class FieldTest {
  @SuppressWarnings("FieldCanBeLocal")
  private char c;

  @Test
  public void testFieldStore() {
    FlowTester tester = new FlowTester();
    c = tester.createSourceChar('a');
    String fieldId =
        FieldRepository.fieldId("com/coekie/flowtracker/test/FieldTest", "c");
    TrackerPoint point = requireNonNull(FieldRepository.getPoint(this, fieldId));
    assertThat(point).isEqualTo(tester.point());
    assertThat(c).isEqualTo('a');
  }

  @Test
  public void testFieldValue() {
    TrackerPoint point = TrackerPoint.of(new FixedOriginTracker(2), 42);
    String fieldId =
        FieldRepository.fieldId("com/coekie/flowtracker/test/FieldTest", "c");
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
