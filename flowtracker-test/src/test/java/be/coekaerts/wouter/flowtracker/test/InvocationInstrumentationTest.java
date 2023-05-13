package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InvocationInstrumentationTest {
  @Test
  public void testReturnValue() {
    FlowTester flowTester = new FlowTester();
    Source source = new Source(flowTester);
    flowTester.assertIsTheTrackedValue((char) source.readInt());
  }

  @Test
  public void testArgValueByte() {
    FlowTester flowTester = new FlowTester();
    Sink sink = new Sink(flowTester);
    sink.writeByte(flowTester.createSourceByte((byte) 1));
    assertTrue(sink.called);
  }

  @Test
  public void testArgValueChar() {
    FlowTester flowTester = new FlowTester();
    Sink sink = new Sink(flowTester);
    sink.writeChar(flowTester.createSourceChar('a'));
    assertTrue(sink.called);
  }

  @Test
  public void testArgValueInt() {
    FlowTester flowTester = new FlowTester();
    Sink sink = new Sink(flowTester);
    sink.writeInt(flowTester.createSourceChar('a'));
    assertTrue(sink.called);
  }

  static class Source {
    final FlowTester flowTester;

    Source(FlowTester flowTester) {
      this.flowTester = flowTester;
    }

    int readInt() {
      return flowTester.createSourceChar('a');
    }
  }

  static class Sink {
    final FlowTester flowTester;
    boolean called;

    Sink(FlowTester flowTester) {
      this.flowTester = flowTester;
    }

    void writeByte(byte b) {
      flowTester.assertIsTheTrackedValue(b);
      called = true;
    }

    void writeChar(char c) {
      flowTester.assertIsTheTrackedValue(c);
      called = true;
    }

    void writeInt(int i) {
      flowTester.assertIsTheTrackedValue((char) i);
      called = true;
    }
  }
}
