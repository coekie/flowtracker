package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Test;

public abstract class AbstractInputStreamTest {
  private static final byte[] abc = "abc".getBytes();

  abstract InputStream createInputStream(byte[] bytes) throws IOException;

  @Test
  public void read() throws IOException {
    try (InputStream is = createInputStream(abc)) {
      byte[] buffer = new byte[5];
      assertEquals(3, is.read(buffer));

      assertContentEquals("abc", is);
      snapshotBuilder().track(is, 0, 3).assertTrackerOf(buffer);
    }
  }

  @Test
  public void readWithOffset() throws IOException {
    try (InputStream fis = createInputStream(abc)) {
      byte[] buffer = new byte[5];
      assertEquals(3, fis.read(buffer, 1, 4));

      assertContentEquals("abc", fis);
      snapshotBuilder().gap(1).track(fis, 0, 3)
          .assertTrackerOf(buffer);
    }
  }

  @Test
  public void readSingleByte() throws IOException {
    try (InputStream fis = createInputStream(abc)) {
      assertEquals('a', fis.read());
      assertContentEquals("a", fis);
    }
  }

  private void assertContentEquals(String expected, InputStream is) {
    var tracker = (ByteOriginTracker) requireNonNull(TrackerRepository.getTracker(is));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }
}