package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.SinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class OutputStreamWriterHook {

  public static void afterInit(OutputStreamWriter target, OutputStream stream) {
    if (Trackers.isActive()) {
      createOutputStreamWriterTracker(target).initDescriptor("OutputStreamWriter",
          TrackerRepository.getTracker(stream));
    }
  }

  static SinkTracker createOutputStreamWriterTracker(OutputStreamWriter target) {
    SinkTracker tracker = new SinkTracker();
    TrackerRepository.setInterestTracker(target, tracker);
    return tracker;
  }

  public static void afterWrite1(OutputStreamWriter target, int c) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      ((SinkTracker) tracker).append((char) c);
      // TODO tracking of source of single character writes
    }
  }

  public static void afterWriteCharArrayOffset(OutputStreamWriter target, char[] cbuf, int off,
      int len) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      Tracker sourceTracker = TrackerRepository.getTracker(cbuf);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((SinkTracker) tracker).append(cbuf, off, len);
    }
  }

  public static void afterWriteStringOffset(OutputStreamWriter target, String str, int off,
      int len) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      Tracker sourceTracker = StringHook.getStringTracker(str);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((SinkTracker) tracker).append(str, off, len);
    }
  }
}
