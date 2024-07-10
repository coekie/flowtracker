package com.coekie.flowtracker.weaver.flow;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class InvocationArgStoreTest {

  @Test
  public void testShouldInstrumentInvocationArg() {
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "whatever", "(B)V"))
        .isTrue();
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "whatever", "(C)V"))
        .isTrue();

    // e.g. OutputStream.write
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "write", "(I)V"))
        .isTrue();
    // e.g. BufferedOutputStream.implWrite
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "implWrite", "(I)V"))
        .isTrue();
    // e.g. PrintStream.print
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "print", "(I)V"))
        .isTrue();

    // java.io.Bits.put* in JDK11
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg(
        "java/io/Bits", "putChar", "([BIC)V"))
        .isTrue();
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg(
        "java/io/Bits", "putShort", "([BIS)V"))
        .isTrue();
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg(
        "java/io/Bits", "putInt", "([BII)V"))
        .isTrue();

    // used in ByteBufferHook
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg(
        "java/nio/ByteBuffer", "putInt", "(I)Ljava/nio/ByteBuffer;"))
        .isTrue();

    // for AsmDemo
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg(
        "org/objectweb/asm/ByteVector", "putInt", "(I)Lorg/objectweb/asm/ByteVector;"))
        .isTrue();
  }
}