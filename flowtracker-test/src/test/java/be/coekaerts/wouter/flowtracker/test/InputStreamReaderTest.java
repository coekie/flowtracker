package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InputStreamReaderTest {
  private final String testFileName =
      '/' + InputStreamReaderTest.class.getName().replace('.', '/') + ".txt";

  private InputStreamReader reader;
  private InputStream stream;

  @Before public void setupReader() {
    stream = InputStreamReaderTest.class.getResourceAsStream(testFileName);
    assertNotNull(stream);
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
    assertEquals(expected,
        requireNonNull(TrackerRepository.getTracker(reader)).getContent().toString());

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
    // i.e. no incomplete read except that the end of the file

    char[] buffer = new char[5];

    // read 3 characters, 0 offset
    assertEquals(3, reader.read(buffer, 0, 3));
    assertArrayEquals(new char[]{'a', 'b', 'c', '\0', '\0'}, buffer);
    snapshotBuilder().track(reader, 0, 3).assertTrackerOf(buffer);
    assertContentEquals("abc");

    // read with offset
    assertEquals(2, reader.read(buffer, 1, 2));
    assertArrayEquals(new char[]{'a', 'd', 'e', '\0', '\0'}, buffer);
    snapshotBuilder().track(reader, 0, 1).track(reader, 3, 2).assertTrackerOf(buffer);
    assertContentEquals("abcde");

    // incomplete read (only 1 instead of 5 asked chars read)
    assertEquals(1, reader.read(buffer, 0, 5));
    assertArrayEquals(new char[]{'f', 'd', 'e', '\0', '\0'}, buffer);
    snapshotBuilder().track(reader, 5, 1).track(reader, 3, 2).assertTrackerOf(buffer);
    assertContentEquals("abcdef");

    // test an extra failed read (eof)
    assertEquals(-1, reader.read(buffer, 0, 5));
    snapshotBuilder().track(reader, 5, 1).track(reader, 3, 2).assertTrackerOf(buffer);
    assertContentEquals("abcdef");
  }

  @Test public void readCharArray() throws IOException {
    char[] buffer1 = new char[1];
    // read 1 char
    assertEquals(1, reader.read(buffer1));
    assertEquals('a', buffer1[0]);
    snapshotBuilder().track(reader, 0, 1).assertTrackerOf(buffer1);
    assertContentEquals("a");

    char[] buffer2 = new char[2];
    // read more
    assertEquals(2, reader.read(buffer2));
    assertArrayEquals(new char[]{'b', 'c'}, buffer2);
    snapshotBuilder().track(reader, 1, 2).assertTrackerOf(buffer2);
    assertContentEquals("abc");

    // and more
    assertEquals(2, reader.read(buffer2));
    // incomplete read (only 1 instead of 2 asked chars read)
    assertEquals(1, reader.read(buffer2));
    assertContentEquals("abcdef");

    // test an extra failed read (eof)
    assertEquals(-1, reader.read(buffer2));
    assertContentEquals("abcdef");
  }

  @Test public void readCharBuffer() throws IOException {
    CharBuffer buffer = CharBuffer.allocate(3);

    assertEquals(3, reader.read(buffer));
    assertContentEquals("abc");

    buffer.position(1);
    assertEquals(2, reader.read(buffer));
    assertContentEquals("abcde");
    buffer.position(1);
    assertEquals(1, reader.read(buffer));
    assertContentEquals("abcdef");

    assertEquals(-1, reader.read(buffer));
    assertContentEquals("abcdef");
  }

  @SuppressWarnings("CharsetObjectCanBeUsed")
  @Test public void interestAndDescriptor() throws IOException {
    assertInterestAndDescriptor(reader);
    assertInterestAndDescriptor(new InputStreamReader(stream, "UTF-8"));
    assertInterestAndDescriptor(new InputStreamReader(stream, StandardCharsets.UTF_8));
    assertInterestAndDescriptor(new InputStreamReader(stream,
        new CharsetDecoder(StandardCharsets.UTF_8, 1, 1) {
          @Override protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            return null;
          }
        }));
  }

  /** Test if stuff in InputStreamHook is applied here */
  @Test public void descriptorForOtherStreams() {
    InputStreamReader reader = new InputStreamReader(new BufferedInputStream(stream));
    TrackTestHelper.assertInterestAndDescriptor(reader, "InputStreamReader",
        InputStreamHook.getInputStreamTracker(stream));
  }

  private void assertInterestAndDescriptor(InputStreamReader reader) {
    TrackTestHelper.assertInterestAndDescriptor(reader, "InputStreamReader",
        InputStreamHook.getInputStreamTracker(stream));
  }
}
