package com.coekie.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker.ClassConstant;
import org.junit.Test;

public class ClassOriginTrackerTest {
  @Test public void testSimpleChar() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass("myClass");
    assertThat(ClassOriginTracker.get(tracker.classId)).isSameInstanceAs(tracker);

    tracker.startMethod("myMethod");
    ClassConstant constant = tracker.registerConstant('x', 7);
    assertThat(tracker.getContent().toString()).isEqualTo("class myClass\n"
            + "myMethod:\n"
            + "  (line 7) x\n");
    assertThat(tracker.getContent().charAt(constant.offset)).isEqualTo('x');
  }

  @Test public void testLargerInt() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass("myClass");
    assertThat(ClassOriginTracker.get(tracker.classId)).isSameInstanceAs(tracker);

    tracker.startMethod("myMethod");
    ClassConstant constant = tracker.registerConstant(9999, 7);
    assertThat(tracker.getContent().toString()).isEqualTo("class myClass\n"
        + "myMethod:\n"
        + "  (line 7) 0x270f (9999)\n");
    assertThat(tracker.getContent().subSequence(constant.offset, constant.offset + constant.length))
        .isEqualTo("0x270f (9999)");
  }
}