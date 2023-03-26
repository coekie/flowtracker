package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
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

  // read() calls the method we've already hooked in afterReadByteArrayOffset; so we've
  // already added it to the tracker. here we just need to propagate the return value.
  // it would have been nice if that worked automatically; it doesn't, probably because of the
  // return with the ternary operator that isn't handled by our instrumentation
  public static void afterRead1(int result, InflaterInputStream target) {
    if (result > 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        Invocation invocation = Invocation.start(InputStreamHook.READ1_SIGNATURE);
        Invocation.returning(invocation, tracker, tracker.getLength() - 1);
      }
    }
  }
}
