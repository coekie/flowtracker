package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Test;

public abstract class AbstractInputStreamTest {
  private static final byte[] abc = "abc".getBytes();

  abstract InputStream createInputStream(byte[] bytes) throws IOException;
  abstract Tracker getStreamTracker(InputStream is);

  @Test
  public void read() throws IOException {
    try (InputStream is = createInputStream(abc)) {
      byte[] buffer = new byte[5];
      assertThat(is.read(buffer)).isEqualTo(3);

      assertContentEquals("abc", is);
      TrackerSnapshot.assertThatTrackerOf(buffer).matches(
          TrackerSnapshot.snapshot().part(3, getStreamTracker(is), 0));
    }
  }

  @Test
  public void readAllBytes() throws IOException {
    try (InputStream is = createInputStream(abc)) {
      byte[] buffer = is.readAllBytes();
      assertContentEquals("abc", is);
      TrackerSnapshot.assertThatTrackerOf(buffer).matches(
          TrackerSnapshot.snapshot().part(3, getStreamTracker(is), 0));
    }
  }

  @Test
  public void readWithOffset() throws IOException {
    try (InputStream fis = createInputStream(abc)) {
      byte[] buffer = new byte[5];
      assertThat(fis.read(buffer, 1, 4)).isEqualTo(3);

      assertContentEquals("abc", fis);
      TrackerSnapshot.assertThatTrackerOf(buffer).matches(
          TrackerSnapshot.snapshot().gap(1).part(3, getStreamTracker(fis), 0));
    }
  }

  @Test
  public void readSingleByte() throws IOException {
    try (InputStream fis = createInputStream(abc)) {
      FlowTester.assertTrackedValue((byte) fis.read(), (byte) 'a', getStreamTracker(fis), 0);
      FlowTester.assertTrackedValue((byte) fis.read(), (byte) 'b', getStreamTracker(fis), 1);
      assertContentEquals("ab", fis);
    }
  }

  void assertContentEquals(String expected, InputStream is) {
    var tracker = (ByteOriginTracker) requireNonNull(getStreamTracker(is));
    assertThat(tracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }
}
