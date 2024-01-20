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

  @Test
  public void twoArgs() {
    FlowTester flowTester0 = new FlowTester();
    FlowTester flowTester1 = new FlowTester();
    class MultiSink {
      boolean called;

      void write(byte b0, byte b1) {
        flowTester0.assertIsTheTrackedValue(b0);
        flowTester1.assertIsTheTrackedValue(b1);
        called = true;
      }
    }

    MultiSink sink = new MultiSink();
    sink.write(
        flowTester0.createSourceByte((byte) 1),
        flowTester1.createSourceByte((byte) 1));
    assertTrue(sink.called);
  }

  @Test
  public void manyArgs() {
    FlowTester flowTester0 = new FlowTester();
    FlowTester flowTester1 = new FlowTester();
    FlowTester flowTester2 = new FlowTester();
    FlowTester flowTester3 = new FlowTester();
    FlowTester flowTester4 = new FlowTester();
    FlowTester flowTester5 = new FlowTester();
    class MultiSink {
      boolean called;

      void write(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5) {
        flowTester0.assertIsTheTrackedValue(b0);
        flowTester1.assertIsTheTrackedValue(b1);
        flowTester2.assertIsTheTrackedValue(b2);
        flowTester3.assertIsTheTrackedValue(b3);
        flowTester4.assertIsTheTrackedValue(b4);
        flowTester5.assertIsTheTrackedValue(b5);
        called = true;
      }
    }

    MultiSink sink = new MultiSink();
    sink.write(
        flowTester0.createSourceByte((byte) 1),
        flowTester1.createSourceByte((byte) 1),
        flowTester2.createSourceByte((byte) 1),
        flowTester3.createSourceByte((byte) 1),
        flowTester4.createSourceByte((byte) 1),
        flowTester5.createSourceByte((byte) 1));
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
