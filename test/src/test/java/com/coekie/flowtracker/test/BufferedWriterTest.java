package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.nullSourceChar;
import static com.coekie.flowtracker.test.TrackTestHelper.trackCopy;

import com.coekie.flowtracker.tracker.TrackerSnapshot;
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
    TrackerSnapshot.assertThatTrackerOf(out).matches(
        TrackerSnapshot.snapshot().trackString(6, str, 0));
    bw.flush();
    TrackerSnapshot.assertThatTrackerOf(out).matches(TrackerSnapshot.snapshot().trackString(str));
  }

  /** write longer than size of the buffer */
  @Test public void longCharArray() throws IOException {
    char[] chars = TrackTestHelper.trackedCharArray("abcdef");
    bw.write(chars, 1, 4);
    TrackerSnapshot.assertThatTrackerOf(out).matches(TrackerSnapshot.snapshot().track(4, chars, 1));
  }

  /** writes shorter than size of the buffer */
  @Test public void shortCharArray() throws IOException {
    char[] chars = TrackTestHelper.trackedCharArray("ab");
    bw.write(chars, 0, 1);
    bw.write(chars, 1, 1);
    bw.write(chars, 0, 2);
    bw.flush();
    TrackerSnapshot.assertThatTrackerOf(out).matches(
        TrackerSnapshot.snapshot().track(2, chars, 0).track(2, chars, 0));
  }

  /** make sure gaps (writes from unknown sources) work properly with the buffer being reused */
  @Test public void gaps() throws IOException {
    String str = trackCopy("abcd");
    bw.write(str);
    bw.write(nullSourceChar('e'));
    bw.write(str);
    bw.flush();
    TrackerSnapshot.assertThatTrackerOf(out).matches(
        TrackerSnapshot.snapshot().trackString(str).gap(1).trackString(str));
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
    outerBw.write(nullSourceChar('c'));
    outerBw.write(str);
    outerBw.flush();
    TrackerSnapshot.assertThatTrackerOf(out).matches(TrackerSnapshot.snapshot()
        .trackString(str)
        .trackString(str)
        .trackString(str)
        .gap(1)
        .trackString(str));
  }
}
