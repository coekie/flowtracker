package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
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

import static org.junit.Assert.assertEquals;

public class OutputStreamWriterTest {

  private OutputStreamWriter writer;
  private OutputStream stream;

  @Before
  public void setupWriter() throws IOException {
    File file = File.createTempFile("OutputStreamWriterTest", "");
    stream = new FileOutputStream(file);
    writer = new OutputStreamWriter(stream);
  }

  @After
  public void close() throws IOException {
    if (writer != null) {
      writer.close();
    }
    if (stream != null) {
      stream.close();
    }
  }

  private void assertContentEquals(String expected) {
    assertEquals(expected,
        ((ContentTracker) TrackerRepository.getTracker(writer)).getContent().toString());
  }

  @Test
  public void writeSingleChar() throws IOException {
    writer.write('a');
    assertContentEquals("a");
    writer.write('b');
    assertContentEquals("ab");
  }

  @Test
  public void writeCharArray() throws IOException {
    writer.write(new char[]{'a', 'b', 'c'});
    assertContentEquals("abc");
  }

  @Test
  public void writeCharArrayOffset() throws IOException {
    writer.write(new char[]{'a', 'b', 'c', 'd'}, 1, 2);
    assertContentEquals("bc");
  }

  @Test
  public void writeString() throws IOException {
    writer.write("abc");
    assertContentEquals("abc");
  }

  @Test
  public void writeStringOffset() throws IOException {
    writer.write("abcd", 1, 2);
    assertContentEquals("bc");
  }

  @Test
  public void interestAndDescriptor() throws IOException {
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
