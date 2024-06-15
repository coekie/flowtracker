package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.ByteArrayOutputStream;

public class ByteArrayOutputStreamTest extends AbstractOutputStreamTest<ByteArrayOutputStream> {

  @Override
  ByteArrayOutputStream createOutputStream() {
    return new ByteArrayOutputStream();
  }

  @Override
  Tracker getTracker(ByteArrayOutputStream os) {
    return TrackerRepository.getTracker(context(), os.toByteArray());
  }

  @Override
  void assertContentEquals(String expected, ByteArrayOutputStream os) {
    assertThat(os.toByteArray()).isEqualTo(expected.getBytes());
  }
}
