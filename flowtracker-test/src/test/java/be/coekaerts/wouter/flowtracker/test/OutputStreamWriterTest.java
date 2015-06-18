package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;

public class OutputStreamWriterTest {

  private OutputStreamWriter writer;
  private OutputStream stream;

  @Before public void setupWriter() throws IOException {
    File file = File.createTempFile("OutputStreamWriterTest", "");
    stream = new FileOutputStream(file);
    writer = new OutputStreamWriter(stream);
  }

  @After public void close() throws IOException {
    if (writer != null) {
      writer.close();
    }
    if (stream != null) {
      stream.close();
    }
  }

  private void assertContentEquals(String expected) {
    assertEquals(expected, TrackerRepository.getTracker(writer).getContent().toString());
  }

  @Test public void writeSingleChar() throws IOException {
    writer.write('a');
    assertContentEquals("a");
    writer.write('b');
    assertContentEquals("ab");
    // tracking of source for write(char) not implemented
    snapshotBuilder().gap(2).assertTrackerOf(writer);
  }

  @Test public void writeCharArray() throws IOException {
    char[] abc = {'a', 'b', 'c'};
    track(abc);
    writer.write(abc);
    writer.write(abc);
    assertContentEquals("abcabc");
    snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertTrackerOf(writer);
  }

  @Test public void writeUntrackedCharArray() throws IOException {
    char[] abc = {'a', 'b', 'c'}; // without tracking
    char[] def = {'d', 'e', 'f'}; // with tracking to test if offset is still correct
    track(def);

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    snapshotBuilder().gap(3).track(def, 0, 3).assertTrackerOf(writer);
  }

  @Test public void writeCharArrayOffset() throws IOException {
    char[] abcd = {'a', 'b', 'c', 'd'};
    track(abcd);
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    snapshotBuilder().track(abcd, 1, 2).assertTrackerOf(writer);
  }

  @Test public void writeString() throws IOException {
    String abc = trackCopy("abc");
    writer.write(abc);
    assertContentEquals("abc");
    snapshotBuilder().trackString(abc).assertTrackerOf(writer);
  }

  @Test public void writeUntrackedString() throws IOException {
    String abc = "abc"; // // without tracking
    String def = trackCopy("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    snapshotBuilder().gap(3).trackString(def).assertTrackerOf(writer);
  }

  @Test public void writeStringOffset() throws IOException {
    String abcd = trackCopy("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    snapshotBuilder().trackString(abcd, 1, 2).assertTrackerOf(writer);
  }

  @Test public void interestAndDescriptor() throws IOException {
    assertInterestAndDescriptor(writer);
    assertInterestAndDescriptor(new OutputStreamWriter(stream, "UTF-8"));
    assertInterestAndDescriptor(new OutputStreamWriter(stream, Charset.forName("UTF-8")));
    assertInterestAndDescriptor(new OutputStreamWriter(stream,
        new CharsetEncoder(Charset.forName("UTF-8"), 1, 1) {
          @Override protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            return null;
          }
        }));
  }

  private void assertInterestAndDescriptor(OutputStreamWriter writer) {
    TrackTestHelper.assertInterestAndDescriptor(writer, "OutputStreamWriter", stream);
  }
}
