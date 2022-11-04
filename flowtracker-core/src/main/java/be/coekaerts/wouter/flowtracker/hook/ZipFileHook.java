package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ZipFileHook {
  public static void afterGetInputStream(InputStream result, ZipFile target, ZipEntry zipEntry) {
    if (Trackers.isActive()) {
      Tracker tracker = TrackerRepository.getTracker(result);
      // shouldn't be null because InflaterInputStream constructor is instrumented
      if (tracker == null) {
        return;
      }

      if (InflaterInputStreamHook.DESCRIPTOR.equals(tracker.getDescriptor())) {
        tracker.replaceDescriptor("Unzipped " + target.getName() + " file " + zipEntry.getName(),
            null);
      }
    }
  }
}
