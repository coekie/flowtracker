package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.trackCopy;
import static com.coekie.flowtracker.test.TrackTestHelper.trackedCharArray;
import static com.coekie.flowtracker.test.TrackTestHelper.untrackedCharArray;
import static com.coekie.flowtracker.test.TrackTestHelper.untrackedString;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.CharContentTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import com.google.common.truth.Truth;
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
    streamTracker = requireNonNull(FileDescriptorTrackerRepository.getWriteTracker(context(),
        stream.getFD()));
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
    Truth.assertThat(
        requireNonNull((CharContentTracker) TrackerRepository.getTracker(context(), writer))
            .getContent().toString())
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
    TrackerSnapshot.assertThatTrackerOf(writer).matches(TrackerSnapshot.snapshot()
        .part(flowTester0.point())
        .part(flowTester1.point()));
    TrackerSnapshot.assertThatTracker(streamTracker).matches(TrackerSnapshot.snapshot()
        .part(flowTester0.point())
        .part(flowTester1.point()));
  }

  @Test public void writeCharArray() throws IOException {
    char[] abc = trackedCharArray("abc");
    writer.write(abc);
    writer.write(abc);
    assertContentEquals("abcabc");
    TrackerSnapshot.assertThatTrackerOf(writer).matches(
        TrackerSnapshot.snapshot().track(3, abc, 0).track(3, abc, 0));
    TrackerSnapshot.assertThatTracker(streamTracker).matches(
        TrackerSnapshot.snapshot().track(3, abc, 0).track(3, abc, 0));
  }

  @Test public void writeUntrackedCharArray() throws IOException {
    char[] abc = untrackedCharArray("abc"); // without tracking
    char[] def = trackedCharArray("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    TrackerSnapshot.assertThatTrackerOf(writer).matches(
        TrackerSnapshot.snapshot().gap(3).track(3, def, 0));
    TrackerSnapshot.assertThatTracker(streamTracker).matches(
        TrackerSnapshot.snapshot().gap(3).track(3, def, 0));
  }

  @Test public void writeCharArrayOffset() throws IOException {
    char[] abcd = trackedCharArray("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    TrackerSnapshot.assertThatTrackerOf(writer).matches(TrackerSnapshot.snapshot().track(2, abcd, 1));
    TrackerSnapshot.assertThatTracker(streamTracker).matches(
        TrackerSnapshot.snapshot().track(2, abcd, 1));
  }

  @Test public void writeString() throws IOException {
    String abc = trackCopy("abc");
    writer.write(abc);
    assertContentEquals("abc");
    TrackerSnapshot.assertThatTrackerOf(writer).matches(TrackerSnapshot.snapshot().trackString(abc));
    TrackerSnapshot.assertThatTracker(streamTracker).matches(TrackerSnapshot.snapshot().trackString(abc));
  }

  @Test public void writeUntrackedString() throws IOException {
    String abc = untrackedString("abc"); // // without tracking
    String def = trackCopy("def"); // with tracking to test if offset is still correct

    writer.write(abc);
    writer.write(def);

    assertContentEquals("abcdef");
    TrackerSnapshot.assertThatTrackerOf(writer).matches(TrackerSnapshot.snapshot().gap(3).trackString(def));
    TrackerSnapshot.assertThatTracker(streamTracker).matches(TrackerSnapshot.snapshot().gap(3).trackString(def));
  }

  @Test public void writeStringOffset() throws IOException {
    String abcd = trackCopy("abcd");
    writer.write(abcd, 1, 2);
    assertContentEquals("bc");
    TrackerSnapshot.assertThatTrackerOf(writer).matches(
        TrackerSnapshot.snapshot().trackString(2, abcd, 1));
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
