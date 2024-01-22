package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;
import java.net.SocketAddress;

/**
 * Hooks for AbstractPlainSocketImpl (jdk 11) and NioSocketImpl (jdk 17+)
 * */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SocketImplHook {
  public static void afterConnect(FileDescriptor fd, SocketAddress remote, int localport) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "Client socket to " + remote + " from " + localport, true, true);
    }
  }
}