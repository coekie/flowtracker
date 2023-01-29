package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertArrayEquals;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BufferedOutputStreamTest extends AbstractOutputStreamTest<BufferedOutputStream> {
  private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

  @Override
  BufferedOutputStream createOutputStream() {
    return new BufferedOutputStream(bout);
  }

  @Override
  Tracker getTracker(BufferedOutputStream os) {
    try {
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return TrackerRepository.getTracker(bout.toByteArray());
  }

  @Override
  void assertContentEquals(String expected, BufferedOutputStream os) {
    try {
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assertArrayEquals(expected.getBytes(), bout.toByteArray());
  }
}