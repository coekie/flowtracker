package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.Reflection;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SocketChannelTest extends AbstractChannelTest<SocketChannel> {
  private static final Field fdField = fdField();

  SocketChannel server;
  SocketChannel client;

  @Before
  public void before() throws IOException {
    try (ServerSocketChannel listenChannel = ServerSocketChannel.open()) {
      listenChannel.socket().bind(null);
      client = SocketChannel.open();
      client.connect(listenChannel.getLocalAddress());
      server = listenChannel.accept();
    }
  }

  @After
  public void after() throws IOException {
    if (server != null) {
      server.close();
    }
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void descriptorForClient() {
    ByteOriginTracker readTracker = getReadTracker(client);
    assertTrue(readTracker.getDescriptor().startsWith("Read Client socket to"));
    assertTrue(InterestRepository.getTrackers().contains(readTracker));

    ByteSinkTracker writeTracker = getWriteTracker(client);
    assertTrue(writeTracker.getDescriptor().startsWith("Write Client socket to"));
    assertTrue(InterestRepository.getTrackers().contains(writeTracker));
  }

  @Test
  public void descriptorForServer() {
    ByteOriginTracker readTracker = getReadTracker(server);
    assertTrue(readTracker.getDescriptor().startsWith("Read Server socket to"));
    assertTrue(InterestRepository.getTrackers().contains(readTracker));

    ByteSinkTracker writeTracker = getWriteTracker(server);
    assertTrue(writeTracker.getDescriptor().startsWith("Write Server socket to"));
    assertTrue(InterestRepository.getTrackers().contains(writeTracker));
  }

  @Override
  SocketChannel openForRead() throws IOException {
    server.write(ByteBuffer.wrap(contentForReading()));
    return client;
  }

  @Override
  SocketChannel openForWrite() {
    return client;
  }

  @Override
  FileDescriptor getFd(SocketChannel channel) {
    return (FileDescriptor) Reflection.getFieldValue(channel, fdField);
  }

  private static Field fdField() {
    try {
      Class<?> clazz = Class.forName("sun.nio.ch.SocketChannelImpl");
      return Reflection.getDeclaredField(clazz, "fd");
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    }
  }
}
