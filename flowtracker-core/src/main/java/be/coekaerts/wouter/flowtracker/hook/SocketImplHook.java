package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;

/**
 * Hooks for AbstractPlainSocketImpl (jdk 11) and NioSocketImpl (jdk 17+)
 * */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SocketImplHook {
  private static final Field fdField = Reflection.getDeclaredField(SocketImpl.class, "fd");
  private static final Field addressField
      = Reflection.getDeclaredField(SocketImpl.class, "address");
  private static final Field portField = Reflection.getDeclaredField(SocketImpl.class, "port");

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "void connect(java.net.SocketAddress,int)")
  @Hook(target = "java.net.AbstractPlainSocketImpl",
      condition = "version < 17",
      method = "void connect(java.net.SocketAddress,int)")
  public static void afterConnect(@Arg("SocketImpl_fd") FileDescriptor fd,
      @Arg("ARG0") SocketAddress remote, @Arg("SocketImpl_localport") int localport) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "Client socket to " + remote + " from " + localport, true, true,
          clientSocketNode(remote));
    }
  }

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "void accept(java.net.SocketImpl)")
  @Hook(target = "java.net.AbstractPlainSocketImpl",
      condition = "version < 17",
      method = "void accept(java.net.SocketImpl)")
  public static void afterAccept(@Arg("ARG0") SocketImpl si,
      @Arg("SocketImpl_localport") int localport) {
    if (Trackers.isActive()) {
      FileDescriptor fd = (FileDescriptor) Reflection.getFieldValue(si, fdField);
      InetAddress address = (InetAddress) Reflection.getFieldValue(si, addressField);
      int port = Reflection.getInt(si, portField);
      FileDescriptorTrackerRepository.createTracker(fd,
          "Server socket to " + localport + " from " + address + ":" + port, true, true,
          serverSocketNode(Integer.toString(localport), address + ":" + port));
    }
  }

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "int tryRead(java.io.FileDescriptor,byte[],int,int)")
  public static void afterTryRead(@Arg("RETURN") int read, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") byte[] buf, @Arg("ARG2") int offset) {
    FileInputStreamHook.afterReadByteArrayOffset(read, fd, buf, offset);
  }

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "int tryWrite(java.io.FileDescriptor,byte[],int,int)")
  public static void afterTryWrite(@Arg("RETURN") int written, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") byte[] buf, @Arg("ARG2") int off) {
    FileOutputStreamHook.afterWriteByteArrayOffset(fd, buf, off, written);
  }

  static TrackerTree.Node serverSocketNode(SocketAddress localAddress, SocketAddress remote) {
    if (localAddress instanceof InetSocketAddress) {
      return serverSocketNode(Integer.toString(((InetSocketAddress) localAddress).getPort()),
          remote.toString());
    } else { // e.g. unix domain sockets? not really tested/supported yet
      return serverSocketNode(localAddress.toString(), remote.toString());
    }
  }

  static TrackerTree.Node serverSocketNode(String localPort, String remote) {
    return TrackerTree.node("Server socket").node(localPort).node(remote);
  }

  static TrackerTree.Node clientSocketNode(SocketAddress remote) {
    return clientSocketNode(remote.toString());
  }

  static TrackerTree.Node clientSocketNode(String remote) {
    return TrackerTree.node("Client socket").node(remote);
  }
}