package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.assertThatTrackerNode;

import com.coekie.flowtracker.hook.Reflection;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
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
  public void nodeForClient() {
    ByteOriginTracker readTracker = getReadTracker(client);
    assertThatTrackerNode(readTracker)
        .hasPathMatching(n -> n.get(0).equals("Client socket")
            && n.get(2).equals(FileDescriptorTrackerRepository.READ));

    ByteSinkTracker writeTracker = getWriteTracker(client);
    assertThatTrackerNode(writeTracker)
        .hasPathMatching(n -> n.get(0).equals("Client socket")
            && n.get(2).equals(FileDescriptorTrackerRepository.WRITE));
  }

  @Test
  public void nodeForServer() {
    ByteOriginTracker readTracker = getReadTracker(server);
    assertThatTrackerNode(readTracker)
        .hasPathMatching(n -> n.get(0).equals("Server socket")
            && n.get(3).equals(FileDescriptorTrackerRepository.READ));

    ByteSinkTracker writeTracker = getWriteTracker(server);
    assertThatTrackerNode(writeTracker)
        .hasPathMatching(n -> n.get(0).equals("Server socket")
            && n.get(3).equals(FileDescriptorTrackerRepository.WRITE));
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
