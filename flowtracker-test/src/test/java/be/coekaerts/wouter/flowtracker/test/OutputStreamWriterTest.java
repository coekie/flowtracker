package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

  private void assertContentEquals(String expected) throws IOException {
    // check content of writer
    assertEquals(expected,
        requireNonNull((CharContentTracker)TrackerRepository.getTracker(writer)).getContent()
            .toString());

    // check content of FileOutputStream
    writer.flush();
    var tracker = (ByteSinkTracker) requireNonNull(TrackerRepository.getTracker(stream));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }

  @Test public void writeSingleChar() throws IOException {
    writer.write('a');
    assertContentEquals("a");
    writer.write('b');
    assertContentEquals("ab");
    // tracking of source for write(char) not implemented
    snapshotBuilder().gap(2).assertTrackerOf(writer);
    snapshotBuilder().gap(2).assertTrackerOf(stream);
  }

  @Test public void writeCharArray() throws IOException {
    char[] abc = trackedCharArray("abc");
    writer.write(abc);
    writer.write(abc);
    assertContentEquals("abcabc");
    snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertTrackerOf(writer);
    snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertTrackerOf(stream);
  }

  @Test public void writeUntrackedCharArray() throws IOException {
    char[] abc = untrackedCharArray("abc"); // without tracking
    char[] def = trackedCharArray("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    snapshotBuilder().gap(3).track(def, 0, 3).assertTrackerOf(writer);
    snapshotBuilder().gap(3).track(def, 0, 3).assertTrackerOf(stream);
  }

  @Test public void writeCharArrayOffset() throws IOException {
    char[] abcd = trackedCharArray("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    snapshotBuilder().track(abcd, 1, 2).assertTrackerOf(writer);
    snapshotBuilder().track(abcd, 1, 2).assertTrackerOf(stream);
  }

  @Test public void writeString() throws IOException {
    String abc = trackCopy("abc");
    writer.write(abc);
    assertContentEquals("abc");
    snapshotBuilder().trackString(abc).assertTrackerOf(writer);
    snapshotBuilder().trackString(abc).assertTrackerOf(stream);
  }

  @Test public void writeUntrackedString() throws IOException {
    String abc = "abc"; // // without tracking
    String def = trackCopy("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    snapshotBuilder().gap(3).trackString(def).assertTrackerOf(writer);
    snapshotBuilder().gap(3).trackString(def).assertTrackerOf(stream);
  }

  @Test public void writeStringOffset() throws IOException {
    String abcd = trackCopy("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    snapshotBuilder().trackString(abcd, 1, 2).assertTrackerOf(writer);
  }

  @SuppressWarnings("CharsetObjectCanBeUsed")
  @Test public void interestAndDescriptor() throws IOException {
    assertInterestAndDescriptor(writer);
    assertInterestAndDescriptor(new OutputStreamWriter(stream, "UTF-8"));
    assertInterestAndDescriptor(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
    assertInterestAndDescriptor(new OutputStreamWriter(stream,
        new CharsetEncoder(StandardCharsets.UTF_8, 1, 1) {
          @Override protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            return null;
          }
        }));
  }

  private void assertInterestAndDescriptor(OutputStreamWriter writer) {
    TrackTestHelper.assertInterestAndDescriptor(writer, "OutputStreamWriter", stream);
  }
}
