package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileOutputStreamHook {
  public static void afterInit(FileDescriptor fd, File file) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "FileOutputStream for " + file.getAbsolutePath(),
          false, true);
    }
  }

  public static void afterWrite1(FileDescriptor fd, int c) {
    ByteSinkTracker tracker = FileDescriptorTrackerRepository.getWriteTracker(fd);
    if (tracker != null) {
      tracker.append((byte) c);
      // TODO tracking of source of single byte writes
    }
  }

  public static void afterWriteByteArray(FileDescriptor fd, byte[] buf) {
    afterWriteByteArrayOffset(fd, buf, 0, buf.length);
  }

  public static void afterWriteByteArrayOffset(FileDescriptor fd, byte[] buf, int off,
      int len) {
    ByteSinkTracker tracker = FileDescriptorTrackerRepository.getWriteTracker(fd);
    if (tracker != null) {
      Tracker sourceTracker = TrackerRepository.getTracker(buf);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      tracker.append(buf, off, len);
    }
  }
}
