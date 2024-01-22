package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public class SocketTest {
  @Test public void testClient() throws IOException {
    try (SocketTester tester = SocketTester.createConnected()) {
      String clientReadDescriptor = SocketTester.getReadTracker(tester.client).getDescriptor();
      assertTrue(clientReadDescriptor.startsWith("Read Client socket to"));
      String clientWriteDescriptor = SocketTester.getWriteTracker(tester.client).getDescriptor();
      assertTrue(clientWriteDescriptor.startsWith("Write Client socket to"));
    }
  }

  @Test public void testServer() throws IOException {
    try (SocketTester tester = SocketTester.createConnected()) {
      String serverReadDescriptor = SocketTester.getReadTracker(tester.server).getDescriptor();
      assertTrue(serverReadDescriptor.startsWith("Read Server socket to"));
      String serverWriteDescriptor = SocketTester.getWriteTracker(tester.server).getDescriptor();
      assertTrue(serverWriteDescriptor.startsWith("Write Server socket to"));
    }
  }
}
