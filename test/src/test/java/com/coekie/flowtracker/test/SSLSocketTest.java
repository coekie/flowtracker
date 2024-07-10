package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;
import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.hook.SSLSocketImplHook;
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
    Tracker readTracker = requireNonNull(
        TrackerRepository.getTracker(context(), tester.client.getInputStream()));
    assertThatTrackerNode(readTracker)
        .hasPathStartingWith("Client socket")
        .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
            "From " + tester.client.getLocalPort(),
            SSLSocketImplHook.READ_SSL);
    Tracker writeTracker = requireNonNull(
        TrackerRepository.getTracker(context(), tester.client.getOutputStream()));
    assertThatTrackerNode(writeTracker)
        .hasPathStartingWith("Client socket")
        .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
            "From " + tester.client.getLocalPort(),
            SSLSocketImplHook.WRITE_SSL);
    assertThat(readTracker.twin()).isEqualTo(writeTracker);
    assertThat(writeTracker.twin()).isEqualTo(readTracker);
  }

  private void testServer(SSLSocketTester tester) throws IOException {
    Tracker readTracker = requireNonNull(
        TrackerRepository.getTracker(context(), tester.server.getInputStream()));
    assertThatTrackerNode(readTracker)
        .hasPathStartingWith("Server socket")
        .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
            tester.server.getRemoteSocketAddress().toString(),
            SSLSocketImplHook.READ_SSL);
    Tracker writeTracker = requireNonNull(
        TrackerRepository.getTracker(context(), tester.server.getOutputStream()));
    assertThatTrackerNode(writeTracker)
        .hasPathStartingWith("Server socket")
        .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
            tester.server.getRemoteSocketAddress().toString(),
            SSLSocketImplHook.WRITE_SSL);
    assertThat(readTracker.twin()).isEqualTo(writeTracker);
    assertThat(writeTracker.twin()).isEqualTo(readTracker);
  }
}
