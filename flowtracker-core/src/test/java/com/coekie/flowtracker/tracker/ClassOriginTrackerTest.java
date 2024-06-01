package com.coekie.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker.ClassConstant;
import com.coekie.flowtracker.tracker.ClassOriginTracker.LineNumberConsumer;
import org.junit.Test;

public class ClassOriginTrackerTest {
  @Test public void registerAndGet() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(null, "myClass");
    assertThat(ClassOriginTracker.get(tracker.classId)).isSameInstanceAs(tracker);
  }

  @Test public void testSimpleChar() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(null, "myClass");

    tracker.startMethod("myMethod");
    ClassConstant constant = tracker.registerConstant('x', 7);
    assertThat(tracker.getContent().toString()).isEqualTo("class myClass\n"
            + "myMethod:\n"
            + "  (line 7) x\n");
    assertThat(tracker.getContent().charAt(constant.offset)).isEqualTo('x');

    MyLineNumberConsumer lineNumbers = new MyLineNumberConsumer();
    tracker.pushLineNumbers(lineNumbers);
    assertThat(lineNumbers.consumed.toString()).isEqualTo("35-36=7 ");
  }

  @Test public void testLargerInt() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(null, "myClass");

    tracker.startMethod("myMethod");
    ClassConstant constant = tracker.registerConstant(9999, 7);
    assertThat(tracker.getContent().toString()).isEqualTo("class myClass\n"
        + "myMethod:\n"
        + "  (line 7) 0x270f (9999)\n");
    assertThat(tracker.getContent().subSequence(constant.offset, constant.offset + constant.length))
        .isEqualTo("0x270f (9999)");

    MyLineNumberConsumer lineNumbers = new MyLineNumberConsumer();
    tracker.pushLineNumbers(lineNumbers);
    assertThat(lineNumbers.consumed.toString()).isEqualTo("35-48=7 ");
  }

  private static class MyLineNumberConsumer implements LineNumberConsumer {
    private final StringBuilder consumed = new StringBuilder();

    @Override
    public void line(int start, int end, int line) {
      consumed.append(start).append('-').append(end).append('=').append(line).append(' ');
    }
  }
}