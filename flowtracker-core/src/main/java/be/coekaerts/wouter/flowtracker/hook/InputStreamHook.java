package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public class InputStreamHook {
  private static final Field filterInputStreamIn =
      Reflection.getDeclaredField(FilterInputStream.class, "in");


  /** Returns the tracker of an input stream, ignoring wrapping FilterInputStreams */
  public static Tracker getInputStreamTracker(InputStream stream) {
    Tracker tracker = TrackerRepository.getTracker(stream);
    if (tracker != null) {
      return tracker;
    } else if (stream instanceof FilterInputStream) {
      return getInputStreamTracker((InputStream) Reflection.getFieldValue(stream, filterInputStreamIn));
    } else {
      return null;
    }
  }
}
