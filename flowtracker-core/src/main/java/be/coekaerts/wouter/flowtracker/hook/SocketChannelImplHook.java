package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SocketChannelImplHook {
  private static final Field remoteAddressField =
      Reflection.getDeclaredField("sun.nio.ch.SocketChannelImpl", "remoteAddress");
  private static final Field localAddressField =
      Reflection.getDeclaredField("sun.nio.ch.SocketChannelImpl", "localAddress");
  private static final Field fdField =
      Reflection.getDeclaredField("sun.nio.ch.SocketChannelImpl", "fd");

  public static void afterConnect(boolean result, SocketChannel channel, FileDescriptor fd) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptorTrackerRepository.createTracker(fd,
          "Client socket to " + remoteAddress + " from " + localAddress, true, true);
    }
  }

  // this actually hooks a method in ServerSocketChannelImpl
  public static void afterFinishAccept(SocketChannel channel, FileDescriptor fd) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptorTrackerRepository.createTracker(fd,
          "Server socket to " + localAddress + " from " + remoteAddress, true, true);
    }
  }

  // this actually hooks a method in ServerSocketChannelImpl
  // for JDK 11
  public static void afterAccept(SocketChannel channel) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptor fd =
          (FileDescriptor) Reflection.getFieldValue(channel, fdField);
      FileDescriptorTrackerRepository.createTracker(fd,
          "Server socket to " + localAddress + " from " + remoteAddress, true, true);
    }
  }
}
