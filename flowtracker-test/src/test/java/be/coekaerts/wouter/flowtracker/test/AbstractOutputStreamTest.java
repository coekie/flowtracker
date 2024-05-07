package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedByteArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedByteArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;

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
      assertThatTracker(getTracker(os)).matches(snapshot()
          .part(flowTester0.point())
          .part(flowTester1.point()));
    }
  }

  @Test public void writeByteArray() throws IOException {
    byte[] abc = trackedByteArray("abc");
    try (var os = createOutputStream()) {
      os.write(abc);
      os.write(abc);

      assertContentEquals("abcabc", os);
      assertThatTracker(getTracker(os)).matches(snapshot().track(3, abc, 0).track(3, abc, 0));
    }
  }

  @Test public void writeUntrackedByteArray() throws IOException {
    byte[] abc = untrackedByteArray("abc"); // without tracking
    byte[] def = trackedByteArray("def"); // with tracking to test if offset is still correct
    try (var os = createOutputStream()) {
      os.write(abc);
      os.write(def);

      assertContentEquals("abcdef", os);
      assertThatTracker(getTracker(os)).matches(snapshot().gap(3).track(3, def, 0));
    }
  }

  @Test public void writeByteArrayOffset() throws IOException {
    byte[] abcd = trackedByteArray("abcd");
    try (var os = createOutputStream()) {
      os.write(abcd, 1, 2);

      assertContentEquals("bc", os);
      assertThatTracker(getTracker(os)).matches(snapshot().track(2, abcd, 1));
    }
  }

  abstract OS createOutputStream() throws IOException;
  abstract Tracker getTracker(OS os);
  abstract void assertContentEquals(String expected, OS os);
}
