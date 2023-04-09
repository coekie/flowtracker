package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.Ignore;

public class ByteArrayInputStreamTest extends AbstractInputStreamTest {

  @Override
  InputStream createInputStream(byte[] bytes) {
    byte[] copy = Arrays.copyOf(bytes, bytes.length);
    TrackerRepository.createFixedOriginTracker(copy, copy.length);
    return new MyByteArrayInputStream(copy);
  }

  @Override
  Tracker getStreamTracker(InputStream is) {
    return TrackerRepository.getTracker(((MyByteArrayInputStream) is).getBuf());
  }

  @Override
  void assertContentEquals(String expected, InputStream is) {
    // we don't track content for ByteArrayInputStream in a ByteOriginTracker
    // (because it's not an origin; its contents must come from somewhere else)
  }

  @Override
  @Ignore // TODO read single byte in ByteArrayInputStream; "return b ? x : y;" problem
  public void readSingleByte() {
  }

  static class MyByteArrayInputStream extends ByteArrayInputStream {
    MyByteArrayInputStream(byte[] buf) {
      super(buf);
    }

    byte[] getBuf() {
      return buf;
    }
  }
}
