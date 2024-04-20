package be.coekaerts.wouter.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ClassOriginTrackerTest {
  @Test public void test() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass("myClass");
    assertThat(ClassOriginTracker.get(tracker.classId)).isSameInstanceAs(tracker);

    tracker.startMethod("myMethod");
    int xOffset = tracker.registerConstant('x');
    assertThat(tracker.getContent().toString()).isEqualTo("class myClass\n"
            + "myMethod:\n"
            + "  x\n");
    assertThat(tracker.getContent().charAt(xOffset)).isEqualTo('x');
  }
}