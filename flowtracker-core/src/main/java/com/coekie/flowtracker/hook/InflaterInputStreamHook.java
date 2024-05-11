package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import com.coekie.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class InflaterInputStreamHook {
  @Hook(target = "java.util.zip.InflaterInputStream",
      method = "void <init>(java.io.InputStream,java.util.zip.Inflater,int)")
  public static void afterInit(@Arg("THIS") InflaterInputStream target,
      @Arg("ARG0") InputStream in) {
    if (Trackers.isActive()) {
      var tracker = new ByteOriginTracker();
      Tracker inTracker = InputStreamHook.getInputStreamTracker(in);
      if (inTracker != null && inTracker.getNode() != null) {
        tracker.addTo(inTracker.getNode().node("Inflater"));
      }
      TrackerRepository.setTracker(target, tracker);
    }
  }

  @Hook(target = "java.util.zip.InflaterInputStream",
      method = "int read(byte[],int,int)")
  public static void afterReadByteArrayOffset(@Arg("RETURN") int read,
      @Arg("THIS") InflaterInputStream target, @Arg("ARG0") byte[] buf, @Arg("ARG1") int offset) {
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
  public static void afterRead1(@Arg("RETURN") int result, @Arg("THIS") InflaterInputStream target,
      @Arg("INVOCATION") Invocation invocation) {
    if (result > 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        Invocation.returning(invocation, TrackerPoint.of(tracker, tracker.getLength() - 1));
      }
    }
  }
}
