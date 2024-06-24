package com.coekie.flowtracker.weaver;

import static com.coekie.flowtracker.tracker.Context.context;
import static com.coekie.flowtracker.weaver.TransformerTestUtils.transformAndRun;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.FakeOriginTracker;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;
import org.junit.Test;
import org.objectweb.asm.Type;

// see also InvocationInstrumentationTest.loadClassAtInvocation
public class SuspendInvocationTransformerTest {
  public static boolean called;

  @Test
  public void testSuspendedInvocation() throws ReflectiveOperationException {
    SuspendInvocationTransformer transformer = new SuspendInvocationTransformer(true);
    called = false;

    Invocation suspendedInvocation = Invocation.create("should get suspended")
        .setArg(0, TrackerPoint.of(new FakeOriginTracker(1000), 777))
        .calling(context());
    transformAndRun(transformer, Type.getType(SuspendInvocationTransformerTestSubject.class));
    assertThat(called).isTrue();
    // invocation should have been restored
    assertThat(Invocation.peekPending()).isSameInstanceAs(suspendedInvocation);
  }
}

// cannot be an inner class because we create a copy of it, and otherwise would get
// "IncompatibleClassChangeError: ... disagree on InnerClasses attribute"
class SuspendInvocationTransformerTestSubject implements Runnable {
  @Override
  public void run() {
    assertThat(Invocation.peekPending()).isNull(); // test that the active invocation was suspended
    SuspendInvocationTransformerTest.called = true;
  }
}