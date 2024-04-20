package be.coekaerts.wouter.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import org.junit.Test;

public class InvocationTest {
  @Test
  public void testReturnValue() {
    Tracker tracker = new CharOriginTracker();
    TrackerPoint trackerPoint = TrackerPoint.of(tracker, 2);

    Invocation callingInvocation = Invocation.createCalling("read");
    // inside the called read() method:
    {
      Invocation calledInvocation = Invocation.start("read");
      assertThat(calledInvocation).isSameInstanceAs(callingInvocation);
      Invocation.returning(calledInvocation, trackerPoint);
    }

    assertThat(callingInvocation.returnPoint).isSameInstanceAs(trackerPoint);
  }

  @Test
  public void testArgumentValue() {
    Tracker tracker = new CharOriginTracker();

    TrackerPoint trackerPoint = TrackerPoint.of(tracker, 2);
    Invocation callingInvocation =
        Invocation.create("write").setArg(0, trackerPoint).calling();
    // inside the called write() method:
    {
      Invocation calledInvocation = requireNonNull(Invocation.start("write"));
      assertThat(calledInvocation).isSameInstanceAs(callingInvocation);
      assertThat(Invocation.getArgPoint(calledInvocation, 0)).isSameInstanceAs(trackerPoint);
    }
  }

  @Test
  public void testStartWithoutCalling() {
    assertThat(Invocation.start("read")).isNull();
  }

  @Test
  public void testUseEachInvocationOnlyOnce() {
    Invocation calling = Invocation.createCalling("read");
    Invocation called = Invocation.start("read");
    assertThat(called).isSameInstanceAs(calling);
    assertThat(Invocation.start("read")).isNull();
  }
}
