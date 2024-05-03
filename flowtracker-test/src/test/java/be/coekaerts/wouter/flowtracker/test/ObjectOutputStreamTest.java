package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class ObjectOutputStreamTest extends AbstractDataOutputStreamTest<ObjectOutputStream> {
  private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

  @Override
  ObjectOutputStream createOutputStream() throws IOException {
    return new ObjectOutputStream(bout);
  }

  @Override
  Tracker getTracker(ObjectOutputStream os) {
    try {
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return TrackerRepository.getTracker(contentWithoutHeader());
  }

  @Override
  void assertContentEquals(String expected, ObjectOutputStream os) {
    try {
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assertThat(contentWithoutHeader()).isEqualTo(expected.getBytes());
  }

  private byte[] contentWithoutHeader() {
    byte[] ba = bout.toByteArray();
    return Arrays.copyOfRange(ba, 6, ba.length);
  }
}
