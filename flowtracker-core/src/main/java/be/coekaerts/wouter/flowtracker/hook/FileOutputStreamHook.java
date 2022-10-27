package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileOutputStream;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileOutputStreamHook {
  public static void afterInit(FileOutputStream target, File file) {
    if (Trackers.isActive()) {
      var tracker = new ByteSinkTracker();
      tracker.initDescriptor("FileOutputStream for " + file.getAbsolutePath(), null);
      TrackerRepository.setTracker(target, tracker);
    }
  }

  public static void afterWrite1(FileOutputStream target, int c) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      ((ByteSinkTracker) tracker).append((byte) c);
      // TODO tracking of source of single byte writes
    }
  }

  public static void afterWriteByteArray(FileOutputStream target, byte[] buf) {
    afterWriteByteArrayOffset(target, buf, 0, buf.length);
  }

  public static void afterWriteByteArrayOffset(FileOutputStream target, byte[] buf, int off,
      int len) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      Tracker sourceTracker = TrackerRepository.getTracker(buf);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((ByteSinkTracker) tracker).append(buf, off, len);
    }
  }
}
