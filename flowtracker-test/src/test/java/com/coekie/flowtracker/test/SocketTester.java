package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.hook.Reflection.clazz;

import com.coekie.flowtracker.hook.Reflection;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;

/** Helper class for testing sockets. Contains a connected server & client socket pair. */
class SocketTester implements Closeable {
  final Socket server;
  final Socket client;

  private SocketTester(Socket server, Socket client) {
    this.server = server;
    this.client = client;
  }

  static SocketTester createConnected() {
    try (ServerSocket listenSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      Socket client = new Socket(listenSocket.getInetAddress(), listenSocket.getLocalPort());
      Socket server = listenSocket.accept();
      return new SocketTester(server, client);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static Tracker getReadTracker(Socket socket) {
    return FileDescriptorTrackerRepository.getReadTracker(getFd(socket));
  }

  static Tracker getWriteTracker(Socket socket) {
    return FileDescriptorTrackerRepository.getWriteTracker(getFd(socket));
  }

  private static FileDescriptor getFd(Socket socket) {
    SocketImpl impl = Reflection.getSlow(Socket.class, "impl", SocketImpl.class, socket);

    // find the fd either in the SocketImpl, or in one that it (SocksSocketImpl) delegates to.
    // if there's a delegation going on depends both on if it's a client or server socket, and the
    // JDK version
    while (true) {
      FileDescriptor fd = Reflection.getSlow(SocketImpl.class, "fd", FileDescriptor.class, impl);
      if (fd != null) {
        return fd;
      }
      if (impl.getClass().getSimpleName().equals("SocksSocketImpl")) {
        impl = Reflection.getSlow(clazz("java.net.SocksSocketImpl"), "delegate",
            SocketImpl.class, impl);
      } else {
        throw new RuntimeException("Cannot find fd");
      }
    }
  }

  @Override
  public void close() throws IOException {
    server.close();
    client.close();
  }
}
