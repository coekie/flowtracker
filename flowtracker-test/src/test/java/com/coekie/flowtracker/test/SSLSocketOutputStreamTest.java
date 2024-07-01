package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.hook.SSLSocketImplHook;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLSocket;
import org.junit.After;

/**
 * Test instrumentation of the OutputStream returned from {@link SSLSocket#getOutputStream()}.
 * Implemented in {@link SSLSocketImplHook}.
 */
public class SSLSocketOutputStreamTest extends AbstractOutputStreamTest<OutputStream> {
  private SSLSocketTester tester;

  @After
  public void after() throws IOException {
    tester.close();
  }

  @Override
  OutputStream createOutputStream() throws IOException {
    tester = SSLSocketTester.createDirect();
    return tester.client.getOutputStream();
  }

  @Override
  Tracker getTracker(OutputStream os) {
    return TrackerRepository.getTracker(context(), os);
  }

  @Override
  void assertContentEquals(String expected, OutputStream os) {
    var tracker = (ByteSinkTracker) requireNonNull(getTracker(os));
    assertThat(tracker.getByteContent()).isEqualTo(ByteBuffer.wrap(expected.getBytes()));
  }
}
