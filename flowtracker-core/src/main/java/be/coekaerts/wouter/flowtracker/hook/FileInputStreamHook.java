package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileInputStream;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileInputStreamHook {
  public static void afterInit(FileInputStream target, File file) {
    if (Trackers.isActive()) {
      TrackerRepository.createContentTracker(target).initDescriptor("FileInputStream for "
        + file.getAbsolutePath(), null);
    }
  }

  // note: there is no hook for the constructor that takes a FileDescriptor
}
