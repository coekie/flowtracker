package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ChannelTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import java.io.FileDescriptor;
import java.nio.ByteBuffer;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class IOUtilHook {
  public static void afterReadByteBuffer(int result, FileDescriptor fd, ByteBuffer dst,
      long position) {
    // TODO do something with position (seeking)
    ByteOriginTracker fdTracker = ChannelTrackerRepository.getReadTracker(fd);
    if (fdTracker != null && result > 0 && !dst.isDirect()) {
      TrackerUpdater.setSourceTracker(dst.array(), dst.position() - result, result, fdTracker,
          fdTracker.getLength());
      fdTracker.append(dst.array(), dst.position() - result, result);
    }
  }
}
