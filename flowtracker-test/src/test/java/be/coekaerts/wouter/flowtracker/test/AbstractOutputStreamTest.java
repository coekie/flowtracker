package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Test;

/** Test instrumentation of {@link FileOutputStream} */
public abstract class AbstractOutputStreamTest<OS extends OutputStream> {

  @Test public void writeSingleByte() throws IOException {
    FlowTester flowTester0 = new FlowTester();
    FlowTester flowTester1 = new FlowTester();
    try (var os = createOutputStream()) {
      os.write(flowTester0.createSourceChar('a'));
      os.write(flowTester1.createSourceChar('b'));

      assertContentEquals("ab", os);
      snapshotBuilder()
          .part(flowTester0.theSource(), flowTester0.theSourceIndex(), 1)
          .part(flowTester1.theSource(), flowTester1.theSourceIndex(), 1)
          .assertEquals(getTracker(os));
    }
  }

  @Test public void writeByteArray() throws IOException {
    byte[] abc = trackedByteArray("abc");
    try (var os = createOutputStream()) {
      os.write(abc);
      os.write(abc);

      assertContentEquals("abcabc", os);
      snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertEquals(getTracker(os));
    }
  }

  @Test public void writeUntrackedByteArray() throws IOException {
    byte[] abc = untrackedByteArray("abc"); // without tracking
    byte[] def = trackedByteArray("def"); // with tracking to test if offset is still correct
    try (var os = createOutputStream()) {
      os.write(abc);
      os.write(def);

      assertContentEquals("abcdef", os);
      snapshotBuilder().gap(3).track(def, 0, 3).assertEquals(getTracker(os));
    }
  }

  @Test public void writeByteArrayOffset() throws IOException {
    byte[] abcd = trackedByteArray("abcd");
    try (var os = createOutputStream()) {
      os.write(abcd, 1, 2);

      assertContentEquals("bc", os);
      snapshotBuilder().track(abcd, 1, 2).assertEquals(getTracker(os));
    }
  }

  abstract OS createOutputStream() throws IOException;
  abstract Tracker getTracker(OS os);
  abstract void assertContentEquals(String expected, OS os);
}
