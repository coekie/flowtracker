package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InputStreamReaderTest {
	private final String testFileName = '/' + InputStreamReaderTest.class.getName().replace('.', '/') + ".txt";
	
	private InputStreamReader reader;
	private InputStream stream;
	
	@Before
	public void setupReader() {
		stream = InputStreamReaderTest.class.getResourceAsStream(testFileName);
		assertNotNull(stream);
		reader = new InputStreamReader(stream);
	}
	
	@After
	public void close() throws IOException {
		if (reader != null) {
			reader.close();
		}
		if (stream != null) {
			stream.close();
		}
	}
	
	private void assertContentEquals(String expected) {
		assertEquals(expected,
        ((ContentTracker) TrackerRepository.getTracker(reader)).getContent().toString());
	}
	
	@Test
	public void readSingleChar() throws IOException {
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
	
	@Test
	public void readCharArrayOffset() throws IOException {
		// we assume in this test that it can always immediately read the asked amount;
		// i.e. no incomplete read except that the end of the file
		
		char buffer[] = new char[5];
		
		// read 3 characters, 0 offset
		assertEquals(3, reader.read(buffer, 0, 3));
		assertTrue(Arrays.equals(new char[] {'a', 'b', 'c', '\0', '\0'}, buffer));
		assertContentEquals("abc");
		
		// read with offset
		assertEquals(2, reader.read(buffer, 1, 2));
		assertTrue(Arrays.equals(new char[] {'a', 'd', 'e', '\0', '\0'}, buffer));
		assertContentEquals("abcde");
		
		// incomplete read (only 1 instead of 5 asked chars read)
		assertEquals(1, reader.read(buffer, 0, 5));
		assertTrue(Arrays.equals(new char[] {'f', 'd', 'e', '\0', '\0'}, buffer));
		assertContentEquals("abcdef");
		
		// test an extra failed read (eof)
		assertEquals(-1, reader.read(buffer, 0, 5));
		assertContentEquals("abcdef");
	}
	
	@Test
	public void readCharArray() throws IOException {
		char buffer1[] = new char[1];
		// read 1 char
		assertEquals(1, reader.read(buffer1));
		assertEquals('a', buffer1[0]);
		assertContentEquals("a");
		
		char buffer2[] = new char[2];
		// read more
		assertEquals(2, reader.read(buffer2));
		assertTrue(Arrays.equals(new char[] {'b', 'c'}, buffer2));
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

	@Test
	public void readCharBuffer() throws IOException {
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
	
	@Test
	public void interestAndDescriptor() throws IOException {
    assertInterestAndDescriptor(reader);
    assertInterestAndDescriptor(new InputStreamReader(stream, "UTF-8"));
    assertInterestAndDescriptor(new InputStreamReader(stream, Charset.forName("UTF-8")));
    assertInterestAndDescriptor(new InputStreamReader(stream,
        new CharsetDecoder(Charset.forName("UTF-8"), 1, 1) {
      @Override protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        return null;
      }
    }));
	}

  @Test public void descriptorForFilteredInputStream() throws IOException {
    // BufferedInputStream is a concrete subclass of FilterInputStream
    InputStreamReader reader = new InputStreamReader(new BufferedInputStream(stream));
    TrackTestHelper.assertInterestAndDescriptor(reader, "InputStreamReader", stream);
  }

  private void assertInterestAndDescriptor(InputStreamReader reader) {
    TrackTestHelper.assertInterestAndDescriptor(reader, "InputStreamReader", stream);
  }
}
