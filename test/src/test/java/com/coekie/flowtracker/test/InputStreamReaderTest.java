package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTrackerOf;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.hook.InputStreamHook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.Tracker;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// we used to handle tracking on the InputStreamReader level, but now it's all handled on
// InputStream, and by tracking through UTF_8$Decoder. This tests if that still works, even though
// we don't have specific instrumentation for InputStreamReader anymore.
public class InputStreamReaderTest {
  private final String testFileName =
      '/' + InputStreamReaderTest.class.getName().replace('.', '/') + ".txt";

  private InputStreamReader reader;
  private InputStream stream;
  private Tracker tracker;

  @Before public void setupReader() {
    stream = InputStreamReaderTest.class.getResourceAsStream(testFileName);
    assertThat(stream).isNotNull();
    tracker = requireNonNull(InputStreamHook.getInputStreamTracker(stream));
    reader = new InputStreamReader(stream);
  }

  @After public void close() throws IOException {
    if (reader != null) {
      reader.close();
    }
    if (stream != null) {
      stream.close();
    }
  }

  private void assertContentEquals(String expected) {
    var tracker = (ByteOriginTracker) requireNonNull(InputStreamHook.getInputStreamTracker(stream));
    // the underlying InputStream may have already read more; so we only test that it starts with
    // the expected content
    ByteBuffer byteContent = tracker.getByteContent();
    ByteBuffer slice = byteContent.slice();
    slice.position(byteContent.position());
    slice.limit(expected.getBytes().length - slice.position());
    assertThat(slice).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }

  @Test public void readSingleChar() throws IOException {
    FlowTester.assertTrackedValue((char) reader.read(), 'a', tracker, 0);
    assertContentEquals("a");
    FlowTester.assertTrackedValue((char) reader.read(), 'b', tracker, 1);
    assertContentEquals("ab");

    // read the rest
    assertThat(reader.read()).isEqualTo('c');
    assertThat(reader.read()).isEqualTo('d');
    assertThat(reader.read()).isEqualTo('e');
    assertThat(reader.read()).isEqualTo('f');
    assertContentEquals("abcdef");

    // test an extra failed read (eof)
    assertThat(reader.read()).isEqualTo(-1);
    assertContentEquals("abcdef");
  }

  @Test public void readCharArrayOffset() throws IOException {
    // we assume in this test that it can always immediately read the asked amount;
    // i.e. no incomplete read except at the end of the file

    char[] buffer = new char[5];

    // read 3 characters, 0 offset
    assertThat(reader.read(buffer, 0, 3)).isEqualTo(3);
    assertThat(buffer).isEqualTo(new char[]{'a', 'b', 'c', '\0', '\0'});
    assertThatTrackerOf(buffer).matches(snapshot().part(3, tracker, 0));
    assertContentEquals("abc");

    // read with offset
    assertThat(reader.read(buffer, 1, 2)).isEqualTo(2);
    assertThat(buffer).isEqualTo(new char[]{'a', 'd', 'e', '\0', '\0'});
    assertThatTrackerOf(buffer).matches(snapshot().part(1, tracker, 0).part(2, tracker, 3));
    assertContentEquals("abcde");

    // incomplete read (only 1 instead of 5 asked chars read)
    assertThat(reader.read(buffer, 0, 5)).isEqualTo(1);
    assertThat(buffer).isEqualTo(new char[]{'f', 'd', 'e', '\0', '\0'});
    assertThatTrackerOf(buffer).matches(snapshot().part(1, tracker, 5).part(2, tracker, 3));
    assertContentEquals("abcdef");

    // test an extra failed read (eof)
    assertThat(reader.read(buffer, 0, 5)).isEqualTo(-1);
    assertThatTrackerOf(buffer).matches(snapshot().part(1, tracker, 5).part(2, tracker, 3));
    assertContentEquals("abcdef");
  }

  @Test public void readCharArray() throws IOException {
    char[] buffer = new char[2];
    assertThat(reader.read(buffer)).isEqualTo(2);
    assertThat(buffer).isEqualTo(new char[]{'a', 'b'});
    assertThatTrackerOf(buffer).matches(snapshot().part(2, tracker, 0));
    assertContentEquals("ab");
  }

  // StreamDecoder has a special case for len==1, treats it like read()
  @Test
  public void readCharArraySize1() throws IOException {
    char[] buffer1 = new char[1];
    // read 1 char
    assertThat(reader.read(buffer1)).isEqualTo(1);
    assertThat(buffer1[0]).isEqualTo('a');
    assertThatTrackerOf(buffer1).matches(snapshot().part(1, tracker, 0));
    assertContentEquals("a");
  }

  @Test public void readCharBuffer() throws IOException {
    CharBuffer buffer = CharBuffer.allocate(3);

    assertThat(reader.read(buffer)).isEqualTo(3);
    assertContentEquals("abc");
    assertThat(buffer.array()).isEqualTo("abc".toCharArray());
    assertThatTrackerOf(buffer.array()).matches(snapshot().part(3, tracker, 0));

    buffer.position(1);
    assertThat(reader.read(buffer)).isEqualTo(2);
    assertContentEquals("abcde");
    assertThat(buffer.array()).isEqualTo("ade".toCharArray());
    assertThatTrackerOf(buffer.array()).matches(snapshot().part(1, tracker, 0).part(2, tracker, 3));

    buffer.position(1);
    assertThat(reader.read(buffer)).isEqualTo(1);
    assertContentEquals("abcdef");
    assertThat(buffer.array()).isEqualTo("afe".toCharArray());
    assertThatTrackerOf(buffer.array()).matches(
        snapshot().part(1, tracker, 0).part(1, tracker, 5).part(1, tracker, 4));

    assertThat(reader.read(buffer)).isEqualTo(-1);
    assertContentEquals("abcdef");
  }
}
