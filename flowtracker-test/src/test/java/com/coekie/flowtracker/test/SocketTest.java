package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;

import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
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
      assertThatTrackerNode(SocketTester.getReadTracker(tester.client))
          .hasPathStartingWith("Client socket")
          .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.READ);
      assertThatTrackerNode(SocketTester.getWriteTracker(tester.client))
          .hasPathStartingWith("Client socket")
          .hasPathEndingWith(tester.client.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.WRITE);
    }
  }

  @Test public void testServer() throws IOException {
    try (SocketTester tester = SocketTester.createConnected()) {
      assertThatTrackerNode(SocketTester.getReadTracker(tester.server))
          .hasPathStartingWith("Server socket")
          .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
              tester.server.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.READ);
      assertThatTrackerNode(SocketTester.getWriteTracker(tester.server))
          .hasPathStartingWith("Server socket")
          .hasPathEndingWith(Integer.toString(tester.server.getLocalPort()),
              tester.server.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.WRITE);
    }
  }
}
