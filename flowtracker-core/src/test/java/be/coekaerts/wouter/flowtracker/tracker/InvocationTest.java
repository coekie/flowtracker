package be.coekaerts.wouter.flowtracker.tracker;

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
