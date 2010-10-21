package be.coekaerts.wouter.flowtracker.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.coekaerts.wouter.flowtracker.history.ContentTracker;
import be.coekaerts.wouter.flowtracker.history.TrackerRepository;

public class InputStreamReaderTest {
	private final String testFileName = '/' + InputStreamReaderTest.class.getName().replace('.', '/') + ".txt";
	
	private InputStreamReader reader;
	private InputStream stream;
	
	@Before
	public void setupReader() {
		stream = InputStreamReaderTest.class.getResourceAsStream(testFileName);
		Assert.assertNotNull(stream);
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
		Assert.assertEquals(expected, ((ContentTracker) TrackerRepository.getTracker(reader)).getContent().toString());
	}
	
	@Test
	public void readStuff() throws IOException {
		Assert.assertEquals('a', reader.read());
		assertContentEquals("a");
		Assert.assertEquals('b', reader.read());
		assertContentEquals("ab");
		
		// read the rest
		Assert.assertEquals('c', reader.read());
		Assert.assertEquals('d', reader.read());
		Assert.assertEquals('e', reader.read());
		Assert.assertEquals('f', reader.read());
		assertContentEquals("abcdef");
		
		// test an extra failed read (eof)
		Assert.assertEquals(-1, reader.read());
		assertContentEquals("abcdef");
	}
	
	@Test
	public void readCharArrayOffset() throws IOException {
		// we assume in this test that it can always directly read the asked amount
		
		char buffer[] = new char[5];
		
		// read 3 characters, 0 offset
		Assert.assertEquals(3, reader.read(buffer, 0, 3));
		Assert.assertTrue(Arrays.equals(new char[]{'a','b','c','\0','\0'}, buffer));
		assertContentEquals("abc");
		
		// read with offset
		Assert.assertEquals(2, reader.read(buffer, 1, 2));
		Assert.assertTrue(Arrays.equals(new char[]{'a','d','e','\0','\0'}, buffer));
		assertContentEquals("abcde");
		
		// incomplete read (only 1 instead of 5 asked chars read)
		Assert.assertEquals(1, reader.read(buffer, 0, 5));
		Assert.assertTrue(Arrays.equals(new char[]{'f','d','e','\0','\0'}, buffer));
		assertContentEquals("abcdef");
		
		// test an extra failed read (eof)
		Assert.assertEquals(-1, reader.read(buffer, 0, 5));
		assertContentEquals("abcdef");
	}
	
	@Test
	public void readCharArray() throws IOException {
		char buffer1[] = new char[1];
		// read 1 char
		Assert.assertEquals(1, reader.read(buffer1));
		Assert.assertEquals('a', buffer1[0]);
		assertContentEquals("a");
		
		char buffer2[] = new char[2];
		// read more
		Assert.assertEquals(2, reader.read(buffer2));
		Assert.assertTrue(Arrays.equals(new char[]{'b','c'}, buffer2));
		assertContentEquals("abc");
		
		// and more
		Assert.assertEquals(2, reader.read(buffer2));
		// incomplete read (only 1 instead of 2 asked chars read)
		Assert.assertEquals(1, reader.read(buffer2));
		assertContentEquals("abcdef");
		
		// test an extra failed read (eof)
		Assert.assertEquals(-1, reader.read(buffer2));
		assertContentEquals("abcdef");
	}
	
	public void readCharBuffer() throws IOException {
		CharBuffer buffer = CharBuffer.allocate(3);
		
		Assert.assertEquals(3, reader.read(buffer));
		assertContentEquals("abc");
		
		buffer.position(1);
		Assert.assertEquals(2, reader.read(buffer));
		assertContentEquals("abcde");
		buffer.position(1);
		Assert.assertEquals(1, reader.read(buffer));
		assertContentEquals("abcdef");
		
		Assert.assertEquals(-1, reader.read(buffer));
		assertContentEquals("abcdef");
	}
	
}
