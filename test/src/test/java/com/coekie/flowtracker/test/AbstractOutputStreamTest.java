package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackedByteArray;
import static com.coekie.flowtracker.test.TrackTestHelper.untrackedByteArray;

import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
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
      TrackerSnapshot.assertThatTracker(getTracker(os)).matches(TrackerSnapshot.snapshot()
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
      TrackerSnapshot.assertThatTracker(getTracker(os)).matches(
          TrackerSnapshot.snapshot().track(3, abc, 0).track(3, abc, 0));
    }
  }

  @Test public void writeUntrackedByteArray() throws IOException {
    byte[] abc = untrackedByteArray("abc"); // without tracking
    byte[] def = trackedByteArray("def"); // with tracking to test if offset is still correct
    try (var os = createOutputStream()) {
      os.write(abc);
      os.write(def);

      assertContentEquals("abcdef", os);
      TrackerSnapshot.assertThatTracker(getTracker(os)).matches(
          TrackerSnapshot.snapshot().gap(3).track(3, def, 0));
    }
  }

  @Test public void writeByteArrayOffset() throws IOException {
    byte[] abcd = trackedByteArray("abcd");
    try (var os = createOutputStream()) {
      os.write(abcd, 1, 2);

      assertContentEquals("bc", os);
      TrackerSnapshot.assertThatTracker(getTracker(os)).matches(
          TrackerSnapshot.snapshot().track(2, abcd, 1));
    }
  }

  abstract OS createOutputStream() throws IOException;
  abstract Tracker getTracker(OS os);
  abstract void assertContentEquals(String expected, OS os);
}
