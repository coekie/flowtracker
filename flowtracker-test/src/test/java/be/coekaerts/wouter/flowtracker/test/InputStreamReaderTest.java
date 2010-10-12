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

import be.coekaerts.wouter.flowtracker.history.ReaderHistory;

public class InputStreamReaderTest {
	private final String testFileName = '/' + InputStreamReaderTest.class.getName().replace('.', '/') + ".txt";
	
	private InputStreamReader reader;
	private ReaderHistory history;
	private InputStream stream;
	
	@Before
	public void setupReader() {
		stream = InputStreamReaderTest.class.getResourceAsStream(testFileName);
		Assert.assertNotNull(stream);
		reader = new InputStreamReader(stream);
		history = ReaderHistory.createHistory(reader, null);
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
	
	@Test
	public void readStuff() throws IOException {
		Assert.assertEquals('a', reader.read());
		Assert.assertEquals("a", history.getReadContent().toString());
		Assert.assertEquals('b', reader.read());
		Assert.assertEquals("ab", history.getReadContent().toString());
		
		// read the rest
		Assert.assertEquals('c', reader.read());
		Assert.assertEquals('d', reader.read());
		Assert.assertEquals('e', reader.read());
		Assert.assertEquals('f', reader.read());
		Assert.assertEquals("abcdef", history.getReadContent().toString());
		
		// test an extra failed read (eof)
		Assert.assertEquals(-1, reader.read());
		Assert.assertEquals("abcdef", history.getReadContent().toString());
	}
	
	@Test
	public void readCharArrayOffset() throws IOException {
		// we assume in this test that it can always directly read the asked amount
		
		char buffer[] = new char[5];
		
		// read 3 characters, 0 offset
		Assert.assertEquals(3, reader.read(buffer, 0, 3));
		Assert.assertTrue(Arrays.equals(new char[]{'a','b','c','\0','\0'}, buffer));
		Assert.assertEquals("abc", history.getReadContent().toString());
		
		// read with offset
		Assert.assertEquals(2, reader.read(buffer, 1, 2));
		Assert.assertTrue(Arrays.equals(new char[]{'a','d','e','\0','\0'}, buffer));
		Assert.assertEquals("abcde", history.getReadContent().toString());
		
		// incomplete read (only 1 instead of 5 asked chars read)
		Assert.assertEquals(1, reader.read(buffer, 0, 5));
		Assert.assertTrue(Arrays.equals(new char[]{'f','d','e','\0','\0'}, buffer));
		Assert.assertEquals("abcdef", history.getReadContent().toString());
		
		// test an extra failed read (eof)
		Assert.assertEquals(-1, reader.read(buffer, 0, 5));
		Assert.assertEquals("abcdef", history.getReadContent().toString());
	}
	
	@Test
	public void readCharArray() throws IOException {
		char buffer1[] = new char[1];
		// read 1 char
		Assert.assertEquals(1, reader.read(buffer1));
		Assert.assertEquals('a', buffer1[0]);
		Assert.assertEquals("a", history.getReadContent().toString());
		
		char buffer2[] = new char[2];
		// read more
		Assert.assertEquals(2, reader.read(buffer2));
		Assert.assertTrue(Arrays.equals(new char[]{'b','c'}, buffer2));
		Assert.assertEquals("abc", history.getReadContent().toString());
		
		// and more
		Assert.assertEquals(2, reader.read(buffer2));
		// incomplete read (only 1 instead of 2 asked chars read)
		Assert.assertEquals(1, reader.read(buffer2));
		Assert.assertEquals("abcdef", history.getReadContent().toString());
		
		// test an extra failed read (eof)
		Assert.assertEquals(-1, reader.read(buffer2));
		Assert.assertEquals("abcdef", history.getReadContent().toString());
	}
	
	public void readCharBuffer() throws IOException {
		CharBuffer buffer = CharBuffer.allocate(3);
		
		Assert.assertEquals(3, reader.read(buffer));
		Assert.assertEquals("abc", history.getReadContent().toString());
		
		buffer.position(1);
		Assert.assertEquals(2, reader.read(buffer));
		Assert.assertEquals("abcde", history.getReadContent().toString());
		buffer.position(1);
		Assert.assertEquals(1, reader.read(buffer));
		Assert.assertEquals("abcdef", history.getReadContent().toString());
		
		Assert.assertEquals(-1, reader.read(buffer));
		Assert.assertEquals("abcdef", history.getReadContent().toString());
	}
	
}
