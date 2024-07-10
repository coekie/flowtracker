package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.hook.SocketImplHook;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
    return FileDescriptorTrackerRepository.getReadTracker(context(),
        SocketImplHook.getSocketFd(socket));
  }

  static Tracker getWriteTracker(Socket socket) {
    return FileDescriptorTrackerRepository.getWriteTracker(context(),
        SocketImplHook.getSocketFd(socket));
  }

  @Override
  public void close() throws IOException {
    server.close();
    client.close();
  }
}
