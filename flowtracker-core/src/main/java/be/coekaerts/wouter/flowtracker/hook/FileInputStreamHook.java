package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileInputStream;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileInputStreamHook {
  public static void afterInit(FileInputStream target, File file) {
    if (Trackers.isActive()) {
      var tracker = new ByteOriginTracker();
      tracker.initDescriptor("FileInputStream for " + file.getAbsolutePath(), null);
      TrackerRepository.setTracker(target, tracker);
    }
  }

  // note: there is no hook for the constructor that takes a FileDescriptor

  public static void afterRead1(int result, FileInputStream target) {
    if (result > 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        ((ByteOriginTracker) tracker).append((byte) result);
      }
    }
  }

  public static void afterReadByteArray(int read, FileInputStream target, byte[] buf) {
    if (read >= 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, 0, read, tracker, tracker.getLength());
        ((ByteOriginTracker) tracker).append(buf, 0, read);
      }
    }
  }

  public static void afterReadByteArrayOffset(int read, FileInputStream target, byte[] buf,
      int offset) {
    if (read >= 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, offset, read, tracker, tracker.getLength());
        ((ByteOriginTracker) tracker).append(buf, offset, read);
      }
    }
  }
}
