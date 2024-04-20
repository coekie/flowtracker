package be.coekaerts.wouter.flowtracker.weaver.flow;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class InvocationArgStoreTest {
  @Test public void testShouldInstrumentInvocationArg() {
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "(B)V")).isTrue();
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "(C)V")).isTrue();

    // e.g. OutputStream.write
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("write", "(I)V")).isTrue();
    // e.g. BufferedOutputStream.implWrite
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("implWrite", "(I)V")).isTrue();
    // e.g. PrintStream.print
    assertThat(InvocationArgStore.shouldInstrumentInvocationArg("print", "(I)V")).isTrue();
  }
}