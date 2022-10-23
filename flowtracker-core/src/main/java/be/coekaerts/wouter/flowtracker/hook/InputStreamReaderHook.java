package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class InputStreamReaderHook {

  public static void afterInit(InputStreamReader target, InputStream source) {
    if (Trackers.isActive()) {
      ContentTracker tracker = new ContentTracker();
      tracker.initDescriptor("InputStreamReader", InputStreamHook.getInputStreamTracker(source));
      TrackerRepository.setInterestTracker(target, tracker);
    }
  }

  public static void afterRead1(int result, InputStreamReader target) {
    if (result > 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        ((ContentTracker) tracker).append((char) result);
      }
    }
  }

  public static void afterReadCharArrayOffset(int read, InputStreamReader target, char[] cbuf,
      int offset) {
    if (read >= 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(cbuf, offset, read, tracker, tracker.getLength());
        ((ContentTracker) tracker).append(cbuf, offset, read);
      }
    }
  }

  public static void afterReadCharBuffer(int read, InputStreamReader target, CharBuffer buf) {
    if (read >= 0) {
      Tracker tracker = TrackerRepository.getTracker(target);
      if (tracker != null) {
        int pos = buf.position();

        // TODO where do we put the tracker of a CharBuffer? on the CharBuffer, or if it's a heap
        //   buffer on the array inside?
        TrackerUpdater.setSourceTracker(buf, pos - read, read, tracker, tracker.getLength());

        buf.position(pos - read);
        ((ContentTracker) tracker).append(buf.subSequence(0, read));
        buf.position(pos);
      }
    }
  }
}
