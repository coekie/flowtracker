package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.net.URLConnection;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class URLConnectionHook {
  public static void afterGetInputStream(InputStream result, URLConnection connection) {
    if (!Trackers.isActive()) return;
    TrackerRepository.createTracker(result)
        .initDescriptor("InputStream from "+ connection.getURL(), null);
  }
}
