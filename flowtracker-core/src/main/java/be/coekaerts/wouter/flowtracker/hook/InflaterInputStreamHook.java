package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class InflaterInputStreamHook {
  public static final String DESCRIPTOR = "InflaterInputStream";

  public static void afterInit(InflaterInputStream target, InputStream in) {
    if (Trackers.isActive()) {
      var tracker = new ByteOriginTracker();
      tracker.initDescriptor(DESCRIPTOR, InputStreamHook.getInputStreamTracker(in));
      TrackerRepository.setTracker(target, tracker);
    }
  }

  public static void afterReadByteArrayOffset(int read, InflaterInputStream target, byte[] buf,
      int offset) {
    if (read > 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, offset, read, tracker, tracker.getLength());
        ((ByteOriginTracker) tracker).append(buf, offset, read);
      }
    }
  }
}
