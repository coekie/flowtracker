package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.hook.SSLSocketImplHook;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import java.io.InputStream;
import javax.net.ssl.SSLSocket;
import org.junit.After;

/**
 * Test instrumentation of the InputStream returned from a {@link SSLSocket#getInputStream()}.
 * Implemented in {@link SSLSocketImplHook}.
 */
public class SSLSocketInputStreamTest extends AbstractInputStreamTest {
  private SSLSocketTester tester;

  @After
  public void after() throws IOException {
    tester.close();
  }

  @Override
  InputStream createInputStream(byte[] bytes) throws IOException {
    tester = SSLSocketTester.createDirect();
    tester.server.getOutputStream().write(bytes);
    tester.server.getOutputStream().close();
    return tester.client.getInputStream();
  }

  @Override
  Tracker getStreamTracker(InputStream is) {
    return TrackerRepository.getTracker(context(), is);
  }
}
