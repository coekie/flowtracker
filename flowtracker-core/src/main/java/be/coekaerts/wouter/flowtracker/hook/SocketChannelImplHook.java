package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
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

  @Hook(target = "sun.nio.ch.SocketChannelImpl",
      method = "boolean connect(java.net.SocketAddress)")
  public static void afterConnect(@Arg("RETURN") boolean result, @Arg("THIS") SocketChannel channel,
      @Arg("SocketChannelImpl_fd") FileDescriptor fd) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.clientSocketNode(remoteAddress));
    }
  }

  @Hook(target = "sun.nio.ch.ServerSocketChannelImpl", // not SocketChannelImpl
      condition = "version > 11",
      method = "java.nio.channels.SocketChannel finishAccept(java.io.FileDescriptor,java.net.SocketAddress)")
  public static void afterFinishAccept(@Arg("RETURN") SocketChannel channel,
      @Arg("ARG0") FileDescriptor fd) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.serverSocketNode(localAddress, remoteAddress));
    }
  }

  @Hook(target = "sun.nio.ch.ServerSocketChannelImpl", // not SocketChannelImpl
      condition = "version <= 11",
      method = "java.nio.channels.SocketChannel accept()")
  public static void afterAccept(@Arg("RETURN") SocketChannel channel) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptor fd =
          (FileDescriptor) Reflection.getFieldValue(channel, fdField);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.serverSocketNode(localAddress, remoteAddress));
    }
  }
}
