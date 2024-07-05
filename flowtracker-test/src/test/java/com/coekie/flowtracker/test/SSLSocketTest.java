package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.SSLSocketImplHook;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.IOException;
import org.junit.Test;

/**
 * @see SocketTest
 */
public class SSLSocketTest {
  @Test public void testClientDirect() throws IOException {
    try (SSLSocketTester tester = SSLSocketTester.createDirect()) {
      testClient(tester);
    }
  }

  @Test public void testServerDirect() throws IOException {
    try (SSLSocketTester tester = SSLSocketTester.createDirect()) {
      testServer(tester);
    }
  }

  @Test public void testClientLayered() throws IOException {
    try (SSLSocketTester tester = SSLSocketTester.createLayered()) {
      testClient(tester);
    }
  }

  @Test public void testServerLayered() throws IOException {
    try (SSLSocketTester tester = SSLSocketTester.createLayered()) {
      testClient(tester);
    }
  }

  private void testClient(SSLSocketTester tester) throws IOException {
    Tracker readTracker = TrackerRepository.getTracker(context(), tester.client.getInputStream());
    assertThatTrackerNode(readTracker)
        .hasPathStartingWith("Client socket")
        .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
            "From " + tester.client.getLocalPort(),
            FileDescriptorTrackerRepository.READ, SSLSocketImplHook.SSL);
    Tracker writeTracker = TrackerRepository.getTracker(context(),
        tester.client.getOutputStream());
    assertThatTrackerNode(writeTracker)
        .hasPathStartingWith("Client socket")
        .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
            "From " + tester.client.getLocalPort(),
            FileDescriptorTrackerRepository.WRITE, SSLSocketImplHook.SSL);
    assertThat(readTracker.twin).isEqualTo(writeTracker);
    assertThat(writeTracker.twin).isEqualTo(readTracker);
  }

  private void testServer(SSLSocketTester tester) throws IOException {
    Tracker readTracker = TrackerRepository.getTracker(context(), tester.server.getInputStream());
    assertThatTrackerNode(readTracker)
        .hasPathStartingWith("Server socket")
        .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
            tester.server.getRemoteSocketAddress().toString(),
            FileDescriptorTrackerRepository.READ, SSLSocketImplHook.SSL);
    Tracker writeTracker = TrackerRepository.getTracker(context(),
        tester.server.getOutputStream());
    assertThatTrackerNode(writeTracker)
        .hasPathStartingWith("Server socket")
        .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
            tester.server.getRemoteSocketAddress().toString(),
            FileDescriptorTrackerRepository.WRITE, SSLSocketImplHook.SSL);
    assertThat(readTracker.twin).isEqualTo(writeTracker);
    assertThat(writeTracker.twin).isEqualTo(readTracker);
  }
}
