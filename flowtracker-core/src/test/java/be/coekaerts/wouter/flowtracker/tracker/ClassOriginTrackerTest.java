package be.coekaerts.wouter.flowtracker.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ClassOriginTrackerTest {
  @Test public void test() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass("myClass");
    assertSame(tracker, ClassOriginTracker.get(tracker.classId));

    int xOffset = tracker.registerConstant("myMethod", 'x');
    assertEquals("class myClass\n"
        + "myMethod literal: x\n",
        tracker.getContent().toString());
    assertEquals('x', tracker.getContent().charAt(xOffset));
  }
}