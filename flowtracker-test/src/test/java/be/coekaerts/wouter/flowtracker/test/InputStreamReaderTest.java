package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
    assertNotNull(stream);
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
    assertEquals(ByteBuffer.wrap(expected.getBytes()), slice);
  }

  @Test public void readSingleChar() throws IOException {
    assertEquals('a', reader.read());
    assertContentEquals("a");
    assertEquals('b', reader.read());
    assertContentEquals("ab");

    // read the rest
    assertEquals('c', reader.read());
    assertEquals('d', reader.read());
    assertEquals('e', reader.read());
    assertEquals('f', reader.read());
    assertContentEquals("abcdef");

    // test an extra failed read (eof)
    assertEquals(-1, reader.read());
    assertContentEquals("abcdef");
  }

  @Test public void readCharArrayOffset() throws IOException {
    // we assume in this test that it can always immediately read the asked amount;
    // i.e. no incomplete read except at the end of the file

    char[] buffer = new char[5];

    // read 3 characters, 0 offset
    assertEquals(3, reader.read(buffer, 0, 3));
    assertArrayEquals(new char[]{'a', 'b', 'c', '\0', '\0'}, buffer);
    snapshotBuilder().part(tracker, 0, 3).assertTrackerOf(buffer);
    assertContentEquals("abc");

    // read with offset
    assertEquals(2, reader.read(buffer, 1, 2));
    assertArrayEquals(new char[]{'a', 'd', 'e', '\0', '\0'}, buffer);
    snapshotBuilder().part(tracker, 0, 1).part(tracker, 3, 2).assertTrackerOf(buffer);
    assertContentEquals("abcde");

    // incomplete read (only 1 instead of 5 asked chars read)
    assertEquals(1, reader.read(buffer, 0, 5));
    assertArrayEquals(new char[]{'f', 'd', 'e', '\0', '\0'}, buffer);
    snapshotBuilder().part(tracker, 5, 1).part(tracker, 3, 2).assertTrackerOf(buffer);
    assertContentEquals("abcdef");

    // test an extra failed read (eof)
    assertEquals(-1, reader.read(buffer, 0, 5));
    snapshotBuilder().part(tracker, 5, 1).part(tracker, 3, 2).assertTrackerOf(buffer);
    assertContentEquals("abcdef");
  }

  @Test public void readCharArray() throws IOException {
    char[] buffer = new char[2];
    assertEquals(2, reader.read(buffer));
    assertArrayEquals(new char[]{'a', 'b'}, buffer);
    snapshotBuilder().part(tracker, 0, 2).assertTrackerOf(buffer);
    assertContentEquals("ab");
  }

  // TODO StreamDecoder has a special case for len==1, treats it like read(), which we don't track
  @Ignore
  @Test
  public void readCharArraySize1() throws IOException {
    char[] buffer1 = new char[1];
    // read 1 char
    assertEquals(1, reader.read(buffer1));
    assertEquals('a', buffer1[0]);
    snapshotBuilder().part(tracker, 0, 1).assertTrackerOf(buffer1);
    assertContentEquals("a");
  }

  @Test public void readCharBuffer() throws IOException {
    CharBuffer buffer = CharBuffer.allocate(3);

    assertEquals(3, reader.read(buffer));
    assertContentEquals("abc");
    // TODO tracking in CharBuffer
    //snapshotBuilder().part(tracker, 1, 3).assertTrackerOf(buffer.array());

    buffer.position(1);
    assertEquals(2, reader.read(buffer));
    assertContentEquals("abcde");
    //snapshotBuilder().part(tracker, 1, 5).assertTrackerOf(buffer.array());
    buffer.position(1);
    assertEquals(1, reader.read(buffer));
    assertContentEquals("abcdef");
    //snapshotBuilder().part(tracker, 1, 6).assertTrackerOf(buffer.array());

    assertEquals(-1, reader.read(buffer));
    assertContentEquals("abcdef");
    //snapshotBuilder().part(tracker, 1, 6).assertTrackerOf(buffer.array());
  }
}
