package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileChannelImplHook {
  public static void afterInit(FileDescriptor fd, String path, boolean readable,
      boolean writeable) {
    if (Trackers.isActive()) {
      if (FileDescriptorTrackerRepository.getReadTracker(fd) == null) {
        FileDescriptorTrackerRepository.createTracker(fd, "FileChannel for " + path, readable,
            writeable);
      }
    }
  }
}
