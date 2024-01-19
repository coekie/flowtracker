package be.coekaerts.wouter.flowtracker.tracker;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class InvocationTest {
  @Test
  public void testReturnValue() {
    Tracker tracker = new CharOriginTracker();

    Invocation callingInvocation = Invocation.calling("read");
    // inside the called read() method:
    {
      Invocation calledInvocation = Invocation.start("read");
      assertSame(callingInvocation, calledInvocation);
      Invocation.returning(calledInvocation, tracker, 2);
    }

    assertEquals(tracker, callingInvocation.returnTracker);
    assertEquals(2, callingInvocation.returnIndex);
  }

  @Test
  public void testArgumentValue() {
    Tracker tracker = new CharOriginTracker();

    TrackerPoint trackerPoint = TrackerPoint.of(tracker, 2);
    Invocation callingInvocation =
        Invocation.calling("write").setArg(0, trackerPoint);
    // inside the called write() method:
    {
      Invocation calledInvocation = requireNonNull(Invocation.start("write"));
      assertSame(callingInvocation, calledInvocation);
      assertSame(trackerPoint, Invocation.getArgPoint(calledInvocation, 0));
      assertEquals(tracker, calledInvocation.arg0Tracker);
      assertEquals(2, calledInvocation.arg0Index);
    }
  }

  @Test
  public void testStartWithoutCalling() {
    assertNull(Invocation.start("read"));
  }

  @Test
  public void testUseEachInvocationOnlyOnce() {
    Invocation calling = Invocation.calling("read");
    Invocation called = Invocation.start("read");
    assertSame(calling, called);
    assertNull(Invocation.start("read"));
  }
}
