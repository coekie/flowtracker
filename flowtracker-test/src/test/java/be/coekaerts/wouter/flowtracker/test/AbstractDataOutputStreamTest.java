package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Test;

/**
 * Abstract test class for OutputStreams that implement DataOutput.
 */
public abstract class AbstractDataOutputStreamTest<OS extends OutputStream & DataOutput>
    extends AbstractOutputStreamTest<OS> {
  @Test
  public void testWriteByte() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeByte(ft.createSourceInt(67));
      assertThatTracker(getTracker(os)).matches(snapshot()
          .part(ft.point()));
    }
  }

  @Test
  public void testWriteChar() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeChar(ft.createSourceInt(0x6162));
      // about the usage of `.simplified()` here:
      // in this test and tests below, what it looks like exactly depends on the JDK version. in
      // older JDKs (<21) it's represented as multiple parts after each other pointing to the same
      // value.
      assertThatTracker(getTracker(os)).simplified().matches(snapshot()
          .part(2, ft.point()));
    }
  }

  @Test
  public void testWriteShort() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeShort(ft.createSourceShort((short) 67));
      assertThatTracker(getTracker(os)).simplified().matches(snapshot()
          .part(2, ft.point()));
    }
  }

  @Test
  public void testWriteInt() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeInt(ft.createSourceInt(0x61626364));
      assertThatTracker(getTracker(os)).simplified().matches(snapshot()
          .part(4, ft.point()));
    }
  }
}
