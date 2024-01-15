package be.coekaerts.wouter.flowtracker.weaver.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InvocationReturnValueTest {
  @Test public void testShouldInstrumentInvocation() {
    assertTrue(InvocationReturnValue.shouldInstrumentInvocation("whatever", "()B"));
    assertTrue(InvocationReturnValue.shouldInstrumentInvocation("whatever", "()C"));

    // e.g. InputStream.read
    assertTrue(InvocationReturnValue.shouldInstrumentInvocation("read", "()I"));
    // e.g. StreamDecoder.read0
    assertTrue(InvocationReturnValue.shouldInstrumentInvocation("read0", "()I"));
    // e.g. StreamDecoder.lockedRead0
    assertTrue(InvocationReturnValue.shouldInstrumentInvocation("lockedRead0", "()I"));

    assertFalse(InvocationReturnValue.shouldInstrumentInvocation(
        "read", "([C)I"));
    assertFalse(InvocationReturnValue.shouldInstrumentInvocation(
        "read", "(Ljava/nio/ByteBuffer;)I"));
  }
}