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
          .part(ft.theSource(), ft.theSourceIndex(), 1));
    }
  }

  @Test
  public void testWriteChar() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeChar(ft.createSourceInt(0x6162));
      if (Runtime.version().feature() >= 21) {
        assertThatTracker(getTracker(os)).matches(snapshot()
            // TODO[growth] should have Growth.DOUBLE
            .part(ft.theSource(), ft.theSourceIndex(), 2));
      } else {
        assertThatTracker(getTracker(os)).matches(snapshot()
            // TODO[growth] would be better if this was one part, with Growth.DOUBLE
            .part(ft.theSource(), ft.theSourceIndex(), 1)
            .part(ft.theSource(), ft.theSourceIndex(), 1));
      }
    }
  }

  @Test
  public void testWriteShort() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeShort(ft.createSourceShort((short) 67));
      if (Runtime.version().feature() >= 21) {
        assertThatTracker(getTracker(os)).matches(snapshot()
            // TODO[growth] should have Growth.DOUBLE
            .part(ft.theSource(), ft.theSourceIndex(), 2));
      } else {
        assertThatTracker(getTracker(os)).matches(snapshot()
            // TODO[growth] would be better if this was one part, with Growth.DOUBLE
            .part(ft.theSource(), ft.theSourceIndex(), 1)
            .part(ft.theSource(), ft.theSourceIndex(), 1));
      }
    }
  }

  @Test
  public void testWriteInt() throws IOException {
    FlowTester ft = new FlowTester();
    try (OS os = createOutputStream()) {
      os.writeInt(ft.createSourceInt(0x61626364));
      if (Runtime.version().feature() >= 21) {
        assertThatTracker(getTracker(os)).matches(snapshot()
            // TODO[growth] should have Growth 4
            .part(ft.theSource(), ft.theSourceIndex(), 4));
      } else {
        assertThatTracker(getTracker(os)).matches(snapshot()
            // TODO[growth] would be better if this was one part, with Growth 4
            .part(ft.theSource(), ft.theSourceIndex(), 1)
            .part(ft.theSource(), ft.theSourceIndex(), 1)
            .part(ft.theSource(), ft.theSourceIndex(), 1)
            .part(ft.theSource(), ft.theSourceIndex(), 1));
      }
    }
  }
}
