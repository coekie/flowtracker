package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TagTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.net.URLConnection;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class URLConnectionHook {
  public static void afterGetInputStream(InputStream result, URLConnection connection) {
    if (!Trackers.isActive()) return;
    Tracker existingTracker = InputStreamHook.getInputStreamTracker(result);
    // (since ZipFile is hooked, the case where existingTracker or descriptor is null is untested)
    if (existingTracker != null) {
      if (existingTracker.getDescriptor() == null) {
        existingTracker.initDescriptor("InputStream from " + connection.getURL());
      }
    } else {
      TrackerRepository.setTracker(result,
          new TagTracker("InputStream from " + connection.getURL()));
    }
  }
}
