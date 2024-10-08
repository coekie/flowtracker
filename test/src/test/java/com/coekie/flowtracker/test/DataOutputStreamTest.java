package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

// this is handled differently in different JDK versions.
// in JDK 11 this is about handling stuff like `(v >>> 8) & 0xFF` in DataOutputStream itself.
// in JDK 21 this is about jdk.internal.util.ByteArray
public class DataOutputStreamTest extends AbstractDataOutputStreamTest<DataOutputStream> {
  private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

  @Override
  DataOutputStream createOutputStream() {
    return new DataOutputStream(bout);
  }

  @Override
  Tracker getTracker(DataOutputStream os) {
    return TrackerRepository.getTracker(context(), bout.toByteArray());
  }

  @Override
  void assertContentEquals(String expected, DataOutputStream os) {
    assertThat(bout.toByteArray()).isEqualTo(expected.getBytes());
  }
}
