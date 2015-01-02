package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FilterInputStream;
import java.io.InputStream;

/** Hook for subclasses of {@link FilterInputStream} */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FilterInputStreamHook {
  public static void afterInit(FilterInputStream target, InputStream source) {
    if (Trackers.isActive()) {
      Tracker sourceTracker = TrackerRepository.getTracker(source);
      if (sourceTracker != null) {
        TrackerRepository.createContentTracker(target)
            .initDescriptor(target.getClass().getSimpleName(), sourceTracker);
      }
    }
  }
}
