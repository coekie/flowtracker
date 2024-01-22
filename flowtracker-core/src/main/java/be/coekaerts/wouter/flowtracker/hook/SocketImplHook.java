package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.InetAddress;
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

  public static void afterConnect(FileDescriptor fd, SocketAddress remote, int localport) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "Client socket to " + remote + " from " + localport, true, true);
    }
  }

  public static void afterAccept(SocketImpl si, int localport) {
    if (Trackers.isActive()) {
      FileDescriptor fd = (FileDescriptor) Reflection.getFieldValue(si, fdField);
      InetAddress address = (InetAddress) Reflection.getFieldValue(si, addressField);
      int port = Reflection.getInt(si, portField);
      FileDescriptorTrackerRepository.createTracker(fd,
          "Server socket to " + localport + " from " + address + ":" + port, true, true);
    }
  }

  public static void afterTryRead(int read, FileDescriptor fd, byte[] buf, int offset) {
    FileInputStreamHook.afterReadByteArrayOffset(read, fd, buf, offset);
  }
}