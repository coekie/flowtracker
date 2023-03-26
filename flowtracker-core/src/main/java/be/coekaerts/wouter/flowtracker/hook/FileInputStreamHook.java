package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileInputStreamHook {
  public static void afterInit(FileDescriptor fd, File file) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "FileInputStream for " + file.getAbsolutePath(),
          true, false);
    }
  }

  // note: there is no hook for the constructor that takes a FileDescriptor

  public static void afterRead1(int result, FileDescriptor fd) {
    if (result > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        Invocation invocation = Invocation.start(InputStreamHook.READ1_SIGNATURE);
        Invocation.returning(invocation, tracker, tracker.getLength());
        tracker.append((byte) result);
      }
    }
  }

  public static void afterReadByteArray(int read, FileDescriptor fd, byte[] buf) {
    if (read > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, 0, read, tracker, tracker.getLength());
        tracker.append(buf, 0, read);
      }
    }
  }

  public static void afterReadByteArrayOffset(int read, FileDescriptor fd, byte[] buf,
      int offset) {
    if (read > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, offset, read, tracker, tracker.getLength());
        tracker.append(buf, offset, read);
      }
    }
  }
}
