package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class OutputStreamWriterHook {

  public static void afterInit(OutputStreamWriter target, OutputStream stream) {
    if (Trackers.isActive()) {
      TrackerRepository.createContentTracker(target).initDescriptor("OutputStreamWriter", stream);
    }
  }

  public static void afterWrite1(OutputStreamWriter target, int c) {
    ContentTracker tracker = TrackerRepository.getContentTracker(target);
    if (tracker != null) {
      tracker.append((char) c);
    }
  }

  public static void afterWriteCharArrayOffset(OutputStreamWriter target, char cbuf[], int off,
      int len) {
    ContentTracker tracker = TrackerRepository.getContentTracker(target);
    if (tracker != null) {
      tracker.append(cbuf, off, len);
    }
  }

  public static void afterWriteStringOffset(OutputStreamWriter target, String str, int off,
      int len) {
    ContentTracker tracker = TrackerRepository.getContentTracker(target);
    if (tracker != null) {
      tracker.append(str, off, len);
    }
  }
}
