package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

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
    assertThat(os.toByteArray()).isEqualTo(expected.getBytes());
  }
}
