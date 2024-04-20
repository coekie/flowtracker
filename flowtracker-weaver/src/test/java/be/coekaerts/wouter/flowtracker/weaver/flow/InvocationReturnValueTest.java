package be.coekaerts.wouter.flowtracker.weaver.flow;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class InvocationReturnValueTest {
  @Test public void testShouldInstrumentInvocation() {
    assertThat(InvocationReturnValue.shouldInstrumentInvocation("whatever", "()B")).isTrue();
    assertThat(InvocationReturnValue.shouldInstrumentInvocation("whatever", "()C")).isTrue();

    // e.g. InputStream.read
    assertThat(InvocationReturnValue.shouldInstrumentInvocation("read", "()I")).isTrue();
    // e.g. StreamDecoder.read0
    assertThat(InvocationReturnValue.shouldInstrumentInvocation("read0", "()I")).isTrue();
    // e.g. StreamDecoder.lockedRead0
    assertThat(InvocationReturnValue.shouldInstrumentInvocation("lockedRead0", "()I")).isTrue();

    assertThat(InvocationReturnValue.shouldInstrumentInvocation(
        "read", "([C)I")).isFalse();
    assertThat(InvocationReturnValue.shouldInstrumentInvocation(
        "read", "(Ljava/nio/ByteBuffer;)I")).isFalse();
  }
}