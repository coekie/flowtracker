package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
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
    assertThat(sink.called).isTrue();
  }

  @Test
  public void testArgValueChar() {
    FlowTester flowTester = new FlowTester();
    Sink sink = new Sink(flowTester);
    sink.writeChar(flowTester.createSourceChar('a'));
    assertThat(sink.called).isTrue();
  }

  @Test
  public void testArgValueInt() {
    FlowTester flowTester = new FlowTester();
    Sink sink = new Sink(flowTester);
    sink.writeInt(flowTester.createSourceChar('a'));
    assertThat(sink.called).isTrue();
  }

  /** Test call using super.method(), that is using INVOKESPECIAL */
  @Test
  public void superCall() {
    FlowTester flowTester = new FlowTester();
    SubSink sink = new SubSink(flowTester);
    sink.writeSuper(flowTester.createSourceByte((byte) 1));
    assertThat(sink.called).isTrue();
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
    assertThat(sink.called).isTrue();
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
    assertThat(sink.called).isTrue();
  }

  /** Tests SuspendInvocationTransformer */
  @Test
  public void loadClassAtInvocation() {
    FlowTester flowTester = new FlowTester();
    // sanity check: StaticSink should not have loaded yet
    assertThat(staticSinkInitialized).isFalse();
    // this call to go with first trigger class loading and initialization before it goes through
    byte result1 = StaticSink.go(flowTester.createSourceByte((byte) 1));
    assertThat(StaticSink.called).isTrue();
    // assert that class loading and initialization didn't disrupt the tracking of the invocation
    flowTester.assertIsTheTrackedValue(result1);
  }

  /**
   * Tests interaction between invocation instrumentation and the ConstantDynamic created for char
   * literals. Regression test for a problem where the initialization of the ConstantDynamic caused
   * the Invocation to get lost due to other Invocations happening in the initialization.
   */
  @Test
  public void constantArgs() {
    class MultiSink {
      boolean called;

      void write(char c0, char c1) {
        assertThat(FlowTester.getCharSourcePoint(c0).tracker)
            .isInstanceOf(ClassOriginTracker.class);
        assertThat(FlowTester.getCharSourcePoint(c1).tracker)
            .isInstanceOf(ClassOriginTracker.class);
        called = true;
      }
    }

    MultiSink sink = new MultiSink();
    sink.write('a', 'b');
    assertThat(sink.called).isTrue();
  }

  static boolean staticSinkInitialized = false;
  static class StaticSink {
    static boolean called;
    static {
      go((byte) 0); // irrelevant call to check that it doesn't "distract" Invocation
      staticSinkInitialized = true;
    }

    static byte go(byte b) {
      called = true;
      return b;
    }
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

  static class SubSink extends Sink {

    SubSink(FlowTester flowTester) {
      super(flowTester);
    }

    void writeSuper(byte b) {
      super.writeByte(b);
    }
  }
}
