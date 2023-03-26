package be.coekaerts.wouter.flowtracker.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import be.coekaerts.wouter.flowtracker.tracker.ShadowStack.Invocation;
import org.junit.Test;

public class ShadowStackTest {
  @Test
  public void testReturnValue() {
    Tracker tracker = new CharOriginTracker();

    Invocation callingInvocation = ShadowStack.calling("read");
    // inside the called read() method:
    {
      Invocation calledInvocation = ShadowStack.start("read");
      assertSame(callingInvocation, calledInvocation);
      Invocation.returning(calledInvocation, tracker, 2);
    }

    assertEquals(tracker, callingInvocation.returnTracker);
    assertEquals(2, callingInvocation.returnIndex);
  }

  @Test
  public void testStartWithoutCalling() {
    assertNull(ShadowStack.start("read"));
  }

  @Test
  public void testUseEachInvocationOnlyOnce() {
    Invocation calling = ShadowStack.calling("read");
    Invocation called = ShadowStack.start("read");
    assertSame(calling, called);
    assertNull(ShadowStack.start("read"));
  }
}
