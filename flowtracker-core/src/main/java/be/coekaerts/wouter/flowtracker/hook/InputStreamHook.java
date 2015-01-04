package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

class InputStreamHook {
  private static final Field filterInputStreamIn;

  static {
    try {
      filterInputStreamIn = FilterInputStream.class.getDeclaredField("in");
      filterInputStreamIn.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new Error(e);
    }
  }

  /** Returns the tracker of an input stream, ignoring wrapping FilterInputStreams */
  static Tracker getInputStreamTracker(InputStream stream) {
    Tracker tracker = TrackerRepository.getTracker(stream);
    if (tracker != null) {
      return tracker;
    } else if (stream instanceof FilterInputStream) {
      try {
        return getInputStreamTracker((InputStream) filterInputStreamIn.get(stream));
      } catch (IllegalAccessException e) {
        throw new Error(e);
      }
    } else {
      return null;
    }
  }
}
