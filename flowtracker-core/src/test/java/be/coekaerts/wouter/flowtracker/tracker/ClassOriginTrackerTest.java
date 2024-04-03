package be.coekaerts.wouter.flowtracker.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ClassOriginTrackerTest {
  @Test public void test() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass("myClass");
    assertSame(tracker, ClassOriginTracker.get(tracker.classId));

    tracker.startMethod("myMethod");
    int xOffset = tracker.registerConstant('x');
    assertEquals("class myClass\n"
            + "myMethod:\n"
            + "  x\n",
        tracker.getContent().toString());
    assertEquals('x', tracker.getContent().charAt(xOffset));
  }
}