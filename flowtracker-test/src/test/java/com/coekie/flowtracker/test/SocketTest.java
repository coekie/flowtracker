package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import java.io.IOException;
import org.junit.Test;

/**
 * Test node / registration in FileDescriptorTrackerRepository for sockets.
 *
 * @see SocketInputStreamTest
 * @see SocketOutputStreamTest
 */
public class SocketTest {
  @Test public void testClient() throws IOException {
    try (SocketTester tester = SocketTester.createConnected()) {
      Tracker readTracker = SocketTester.getReadTracker(tester.client);
      assertThatTrackerNode(readTracker)
          .hasPathStartingWith("Client socket")
          .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.READ);
      Tracker writeTracker = SocketTester.getWriteTracker(tester.client);
      assertThatTrackerNode(writeTracker)
          .hasPathStartingWith("Client socket")
          .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.WRITE);
      assertThat(readTracker.twin).isEqualTo(writeTracker);
      assertThat(writeTracker.twin).isEqualTo(readTracker);
    }
  }

  @Test public void testServer() throws IOException {
    try (SocketTester tester = SocketTester.createConnected()) {
      Tracker readTracker = SocketTester.getReadTracker(tester.server);
      assertThatTrackerNode(readTracker)
          .hasPathStartingWith("Server socket")
          .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
              tester.server.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.READ);
      Tracker writeTracker = SocketTester.getWriteTracker(tester.server);
      assertThatTrackerNode(writeTracker)
          .hasPathStartingWith("Server socket")
          .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
              tester.server.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.WRITE);
      assertThat(readTracker.twin).isEqualTo(writeTracker);
      assertThat(writeTracker.twin).isEqualTo(readTracker);
    }
  }
}
