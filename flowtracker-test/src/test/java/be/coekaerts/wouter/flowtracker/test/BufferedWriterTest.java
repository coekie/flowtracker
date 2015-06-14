package be.coekaerts.wouter.flowtracker.test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.junit.Before;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.gap;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.strPart;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;

public class BufferedWriterTest {

  private OutputStreamWriter out;
  private BufferedWriter bw;

  @Before public void setUp() throws Exception {
    out = new OutputStreamWriter(new ByteArrayOutputStream());
    bw = new BufferedWriter(out, 3);
  }

  @Test public void string() throws IOException {
    String str = trackCopy("abcdefg");
    bw.write(str);
    TrackTestHelper.assertPartsCompleteEqual(out, strPart(str, 0, 6));
    bw.flush();
    TrackTestHelper.assertPartsCompleteEqual(out, strPart(str));
  }

  /** write longer than size of the buffer */
  @Test public void longCharArray() throws IOException {
    char[] chars = {'a', 'b', 'c', 'd', 'e', 'f'};
    track(chars);
    bw.write(chars, 1, 4);
    TrackTestHelper.assertPartsCompleteEqual(out, part(chars, 1, 4));
  }

  /** writes shorter than size of the buffer */
  @Test public void shortCharArray() throws IOException {
    char[] chars = {'a', 'b'};
    track(chars);
    bw.write(chars, 0, 1);
    bw.write(chars, 1, 1);
    bw.write(chars, 0, 2);
    bw.flush();
    TrackTestHelper.assertPartsCompleteEqual(out, part(chars, 0, 2), part(chars, 0, 2));
  }

  /** make sure gaps (writes from unknown sources) work properly with the buffer being reused */
  @Test public void gaps() throws IOException {
    String str = trackCopy("abcd");
    bw.write(str);
    bw.write('e');
    bw.write(str);
    bw.flush();
    TrackTestHelper.assertPartsCompleteEqual(out, strPart(str), gap(1), strPart(str));
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
    outerBw.flush();
    TrackTestHelper.assertPartsCompleteEqual(out, strPart(str), strPart(str), strPart(str), gap(1));
  }
}
