package com.coekie.flowtracker.test;

import static com.google.common.truth.Truth.assertWithMessage;

import com.coekie.flowtracker.hook.NetSocketInputStreamHook;
import com.coekie.flowtracker.hook.SocketImplHook;
import com.coekie.flowtracker.tracker.Tracker;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import org.junit.After;

/**
 * Test instrumentation of the InputStream returned from {@link Socket#getInputStream()}.
 * For JDK11 that involves {@link NetSocketInputStreamHook}, for JDK17+ that is through
 * {@link SocketImplHook#afterTryRead(int, FileDescriptor, byte[], int)}.
 */
public class SocketInputStreamTest extends AbstractInputStreamTest {
  private SocketTester tester;

  @After
  public void after() throws IOException {
    tester.close();
  }

  @Override
  InputStream createInputStream(byte[] bytes) throws IOException {
    tester = SocketTester.createConnected();
    tester.server.getOutputStream().write(bytes);
    tester.server.getOutputStream().close();
    InputStream is = tester.client.getInputStream();
    // not sure if this is needed, but just to be safe, to avoid test flakiness
    while (is.available() == 0) {
      try {
        //noinspection BusyWait
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return is;
  }

  @Override
  Tracker getStreamTracker(InputStream is) {
    try {
      assertWithMessage("this test only supports the one InputStream")
          .that(is).isSameInstanceAs(tester.client.getInputStream());
      return SocketTester.getReadTracker(tester.client);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
