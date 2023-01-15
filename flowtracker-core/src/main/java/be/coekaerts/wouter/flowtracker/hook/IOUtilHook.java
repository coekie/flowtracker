package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import java.io.FileDescriptor;
import java.nio.ByteBuffer;

// IOUtil is used by e.g. FileChannelImpl. This is tested in FileChannelTest.
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class IOUtilHook {
  public static void afterReadByteBuffer(int result, FileDescriptor fd, ByteBuffer dst,
      long position) {
    // TODO do something with position (seeking)
    ByteOriginTracker fdTracker = FileDescriptorTrackerRepository.getReadTracker(fd);
    if (fdTracker != null && result > 0 && !dst.isDirect()) {
      TrackerUpdater.setSourceTracker(dst.array(), dst.position() - result, result, fdTracker,
          fdTracker.getLength());
      fdTracker.append(dst.array(), dst.position() - result, result);
    }
  }

  public static void afterWriteByteBuffer(int result, FileDescriptor fd, ByteBuffer src,
      long position) {
    // TODO do something with position (seeking)
    ByteSinkTracker fdTracker = FileDescriptorTrackerRepository.getWriteTracker(fd);
    if (fdTracker != null && result > 0 && !src.isDirect()) {
      Tracker srcTracker = TrackerRepository.getTracker(src.array());
      if (srcTracker != null) {
        fdTracker.setSource(fdTracker.getLength(), result, srcTracker, src.position() - result);
      }
      fdTracker.append(src.array(), src.position() - result, result);
    }
  }
}
