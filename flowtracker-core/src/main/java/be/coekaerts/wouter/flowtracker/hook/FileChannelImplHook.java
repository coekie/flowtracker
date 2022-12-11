package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ChannelTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.nio.channels.FileChannel;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileChannelImplHook {
  public static void afterInit(FileChannel target, String path, boolean readable,
      boolean writeable) {
    if (Trackers.isActive()) {
      ChannelTrackerRepository.createTracker(target, path, readable, writeable);
    }
  }
}
