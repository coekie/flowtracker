package be.coekaerts.wouter.flowtracker.weaver;

import static be.coekaerts.wouter.flowtracker.weaver.TransformerTestUtils.transformAndRun;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import org.junit.Test;
import org.objectweb.asm.Type;

// see also InvocationInstrumentationTest.loadClassAtInvocation
public class SuspendInvocationTransformerTest {
  public static boolean called;

  @Test
  public void testSuspendedInvocation() throws ReflectiveOperationException {
    SuspendInvocationTransformer transformer = new SuspendInvocationTransformer(true);
    called = false;

    Invocation suspendedInvocation = Invocation.createCalling("should get suspended")
        .setArg(0, TrackerPoint.of(new FixedOriginTracker(1000), 777));
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