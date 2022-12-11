package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertArrayEquals;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.ByteArrayOutputStream;

public class ByteArrayOutputStreamTest extends AbstractOutputStreamTest<ByteArrayOutputStream> {

  @Override
  ByteArrayOutputStream createOutputStream() {
    return new ByteArrayOutputStream();
  }

  @Override
  Tracker getTracker(ByteArrayOutputStream os) {
    return TrackerRepository.getTracker(os.toByteArray());
  }

  @Override
  void assertContentEquals(String expected, ByteArrayOutputStream os) {
    byte[] bytes = os.toByteArray();
    assertArrayEquals(expected.getBytes(), bytes);
  }
}
