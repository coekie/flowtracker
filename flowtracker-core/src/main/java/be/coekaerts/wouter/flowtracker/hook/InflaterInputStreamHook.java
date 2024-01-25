package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class InflaterInputStreamHook {
  public static final String DESCRIPTOR = "InflaterInputStream";

  @Hook(target = "java.util.zip.InflaterInputStream",
      method = "void <init>(java.io.InputStream,java.util.zip.Inflater,int)")
  public static void afterInit(@Arg("THIS") InflaterInputStream target,
      @Arg("ARG0") InputStream in) {
    if (Trackers.isActive()) {
      var tracker = new ByteOriginTracker();
      tracker.initDescriptor(DESCRIPTOR, InputStreamHook.getInputStreamTracker(in));
      TrackerRepository.setTracker(target, tracker);
    }
  }

  @Hook(target = "java.util.zip.InflaterInputStream",
      method = "int read(byte[],int,int)")
  public static void afterReadByteArrayOffset(int read, @Arg("THIS") InflaterInputStream target,
      @Arg("ARG0") byte[] buf, @Arg("ARG1") int offset) {
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
  @Hook(target = "java.util.zip.InflaterInputStream",
      method = "int read()")
  public static void afterRead1(int result, @Arg("THIS") InflaterInputStream target,
      @Arg("INVOCATION") Invocation invocation) {
    if (result > 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        Invocation.returning(invocation, TrackerPoint.of(tracker, tracker.getLength() - 1));
      }
    }
  }
}
