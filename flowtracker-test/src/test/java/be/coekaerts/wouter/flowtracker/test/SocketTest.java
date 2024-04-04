package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertThatTracker;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
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
      assertThatTracker(SocketTester.getReadTracker(tester.client))
          .hasNodeStartingWith("Client socket")
          .hasNodeEndingWith(tester.client.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.READ);
      assertThatTracker(SocketTester.getWriteTracker(tester.client))
          .hasNodeStartingWith("Client socket")
          .hasNodeEndingWith(tester.client.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.WRITE);
    }
  }

  @Test public void testServer() throws IOException {
    try (SocketTester tester = SocketTester.createConnected()) {
      assertThatTracker(SocketTester.getReadTracker(tester.server))
          .hasNodeStartingWith("Server socket")
          .hasNodeEndingWith(Integer.toString(tester.server.getLocalPort()),
              tester.server.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.READ);
      assertThatTracker(SocketTester.getWriteTracker(tester.server))
          .hasNodeStartingWith("Server socket")
          .hasNodeEndingWith(Integer.toString(tester.server.getLocalPort()),
              tester.server.getRemoteSocketAddress().toString(),
              FileDescriptorTrackerRepository.WRITE);
    }
  }
}
