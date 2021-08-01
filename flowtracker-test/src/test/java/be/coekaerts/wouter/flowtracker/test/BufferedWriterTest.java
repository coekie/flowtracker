package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.junit.Before;
import org.junit.Test;

public class BufferedWriterTest {

  private OutputStreamWriter out;
  private BufferedWriter bw;

  @Before public void setUp() {
    out = new OutputStreamWriter(new ByteArrayOutputStream());
    bw = new BufferedWriter(out, 3);
  }

  @Test public void string() throws IOException {
    String str = trackCopy("abcdefg");
    bw.write(str);
    snapshotBuilder().trackString(str, 0, 6).assertTrackerOf(out);
    bw.flush();
    snapshotBuilder().trackString(str).assertTrackerOf(out);
  }

  /** write longer than size of the buffer */
  @Test public void longCharArray() throws IOException {
    char[] chars = TrackTestHelper.trackedCharArray("abcdef");
    bw.write(chars, 1, 4);
    snapshotBuilder().track(chars, 1, 4).assertTrackerOf(out);
  }

  /** writes shorter than size of the buffer */
  @Test public void shortCharArray() throws IOException {
    char[] chars = TrackTestHelper.trackedCharArray("ab");
    bw.write(chars, 0, 1);
    bw.write(chars, 1, 1);
    bw.write(chars, 0, 2);
    bw.flush();
    snapshotBuilder().track(chars, 0, 2).track(chars, 0, 2).assertTrackerOf(out);
  }

  /** make sure gaps (writes from unknown sources) work properly with the buffer being reused */
  @Test public void gaps() throws IOException {
    String str = trackCopy("abcd");
    bw.write(str);
    bw.write('e');
    bw.write(str);
    bw.flush();
    snapshotBuilder().trackString(str).gap(1).trackString(str).assertTrackerOf(out);
  }

  @Test public void gapThroughMultipleBuffers() throws IOException {
    BufferedWriter outerBw = new BufferedWriter(bw, 3);
    String str = trackCopy("a");
    outerBw.write(str);
    outerBw.write(str);
    outerBw.flush(); // flushing before buffer is full to avoid writing straight through bw
    outerBw.write(str);
    // testing that a gap also gets properly written *out* of a BufferedWriter
    // (from outerBw into bw, overwriting the previous tracker there)
    outerBw.write('c');
    outerBw.write(str);
    outerBw.flush();
    snapshotBuilder().trackString(str).trackString(str).trackString(str).gap(1).trackString(str)
        .assertTrackerOf(out);
  }
}
