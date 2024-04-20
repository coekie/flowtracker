package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedCharArray;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.untrackedString;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
  private FileOutputStream stream;
  private ByteSinkTracker streamTracker;
  private File file;

  @Before public void setupWriter() throws IOException {
    file = File.createTempFile("OutputStreamWriterTest", "");
    stream = new FileOutputStream(file);
    writer = new OutputStreamWriter(stream);
    streamTracker = requireNonNull(FileDescriptorTrackerRepository.getWriteTracker(stream.getFD()));
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
    assertThat(
        requireNonNull((CharContentTracker) TrackerRepository.getTracker(writer)).getContent()
            .toString())
        .isEqualTo(expected);

    // check content of FileOutputStream
    writer.flush();
    assertThat(streamTracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }

  @Test public void writeSingleChar() throws IOException {
    FlowTester flowTester0 = new FlowTester();
    FlowTester flowTester1 = new FlowTester();
    writer.write(flowTester0.createSourceChar('a'));
    assertContentEquals("a");
    writer.write(flowTester1.createSourceChar('b'));
    assertContentEquals("ab");
    snapshotBuilder()
        .part(flowTester0.theSource(), flowTester0.theSourceIndex(), 1)
        .part(flowTester1.theSource(), flowTester1.theSourceIndex(), 1)
        .assertTrackerOf(writer);
    snapshotBuilder()
        .part(flowTester0.theSource(), flowTester0.theSourceIndex(), 1)
        .part(flowTester1.theSource(), flowTester1.theSourceIndex(), 1)
        .assertEquals(streamTracker);
  }

  @Test public void writeCharArray() throws IOException {
    char[] abc = trackedCharArray("abc");
    writer.write(abc);
    writer.write(abc);
    assertContentEquals("abcabc");
    snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertTrackerOf(writer);
    snapshotBuilder().track(abc, 0, 3).track(abc, 0, 3).assertEquals(streamTracker);
  }

  @Test public void writeUntrackedCharArray() throws IOException {
    char[] abc = untrackedCharArray("abc"); // without tracking
    char[] def = trackedCharArray("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    snapshotBuilder().gap(3).track(def, 0, 3).assertTrackerOf(writer);
    snapshotBuilder().gap(3).track(def, 0, 3).assertEquals(streamTracker);
  }

  @Test public void writeCharArrayOffset() throws IOException {
    char[] abcd = trackedCharArray("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    snapshotBuilder().track(abcd, 1, 2).assertTrackerOf(writer);
    snapshotBuilder().track(abcd, 1, 2).assertEquals(streamTracker);
  }

  @Test public void writeString() throws IOException {
    String abc = trackCopy("abc");
    writer.write(abc);
    assertContentEquals("abc");
    snapshotBuilder().trackString(abc).assertTrackerOf(writer);
    snapshotBuilder().trackString(abc).assertEquals(streamTracker);
  }

  @Test public void writeUntrackedString() throws IOException {
    String abc = untrackedString("abc"); // // without tracking
    String def = trackCopy("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    snapshotBuilder().gap(3).trackString(def).assertTrackerOf(writer);
    snapshotBuilder().gap(3).trackString(def).assertEquals(streamTracker);
  }

  @Test public void writeStringOffset() throws IOException {
    String abcd = trackCopy("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    snapshotBuilder().trackString(abcd, 1, 2).assertTrackerOf(writer);
  }

  @SuppressWarnings("CharsetObjectCanBeUsed")
  @Test public void node() throws IOException {
    assertNode(writer);
    assertNode(new OutputStreamWriter(stream, "UTF-8"));
    assertNode(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
    assertNode(new OutputStreamWriter(stream,
        new CharsetEncoder(StandardCharsets.UTF_8, 1, 1) {
          @Override protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            return null;
          }
        }));
    assertNode(new OutputStreamWriter(new BufferedOutputStream(stream)));
  }

  private void assertNode(OutputStreamWriter writer) {
    TrackTestHelper.assertThatTrackerNode(writer)
        .hasPathStartingWith("Files")
        .hasPathEndingWith(file.getName(), "Writer");
  }
}
