package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import org.junit.After;

/** Test instrumentation of the OutputStream returned from {@link Socket#getOutputStream()}. */
public class SocketOutputStreamTest extends AbstractOutputStreamTest<OutputStream> {
  private SocketTester tester;

  @After
  public void after() throws IOException {
    tester.close();
  }

  @Override
  OutputStream createOutputStream() throws IOException {
    tester = SocketTester.createConnected();
    return tester.client.getOutputStream();
  }

  @Override
  Tracker getTracker(OutputStream os) {
    try {
      assertSame("this test only supports the one OutputStream",
          tester.client.getOutputStream(), os);
      return SocketTester.getWriteTracker(tester.client);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  void assertContentEquals(String expected, OutputStream os) {
    var tracker = (ByteSinkTracker) requireNonNull(getTracker(os));
    assertEquals(ByteBuffer.wrap(expected.getBytes()), tracker.getByteContent());
  }
}
