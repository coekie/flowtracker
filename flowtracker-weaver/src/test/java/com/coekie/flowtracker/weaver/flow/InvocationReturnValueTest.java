package com.coekie.flowtracker.weaver.flow;

import static com.coekie.flowtracker.weaver.flow.InvocationReturnValue.shouldInstrumentInvocation;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class InvocationReturnValueTest {
  @Test public void testShouldInstrumentInvocation() {
    assertThat(shouldInstrumentInvocation("", "whatever", "()B")).isTrue();
    assertThat(shouldInstrumentInvocation("", "whatever", "()C")).isTrue();

    // e.g. InputStream.read
    assertThat(shouldInstrumentInvocation("", "read", "()I")).isTrue();
    // e.g. StreamDecoder.read0
    assertThat(shouldInstrumentInvocation("", "read0", "()I")).isTrue();
    // e.g. StreamDecoder.lockedRead0
    assertThat(shouldInstrumentInvocation("", "lockedRead0", "()I")).isTrue();

    // Character.codePointAt,toCodePoint
    assertThat(shouldInstrumentInvocation("", "codePointAt", "([CII)I")).isTrue();
    assertThat(shouldInstrumentInvocation("", "toCodePoint", "(CC)I")).isTrue();
    assertThat(shouldInstrumentInvocation("", "offsetByCodePoints", "([CIII)I")).isFalse();

    assertThat(shouldInstrumentInvocation("", "read", "([C)I")).isFalse();
    assertThat(shouldInstrumentInvocation("", "read", "(Ljava/nio/ByteBuffer;)I")).isFalse();

    assertThat(shouldInstrumentInvocation("java/lang/String", "charAt", "(I)C")).isFalse();
  }
}