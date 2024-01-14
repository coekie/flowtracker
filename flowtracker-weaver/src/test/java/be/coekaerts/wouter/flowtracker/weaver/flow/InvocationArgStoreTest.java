package be.coekaerts.wouter.flowtracker.weaver.flow;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InvocationArgStoreTest {
  @Test public void testShouldInstrumentInvocationArg() {
    assertTrue(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "(B)V"));
    assertTrue(InvocationArgStore.shouldInstrumentInvocationArg("whatever", "(C)V"));

    // e.g. OutputStream.write
    assertTrue(InvocationArgStore.shouldInstrumentInvocationArg("write", "(I)V"));
    // BufferedOutputStream.implWrite
    assertTrue(InvocationArgStore.shouldInstrumentInvocationArg("implWrite", "(I)V"));
    // (I forgot where we needed this)
    assertTrue(InvocationArgStore.shouldInstrumentInvocationArg("print", "(I)V"));
  }
}