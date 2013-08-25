package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.InputStream;
import java.net.URL;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class URLHook {
  public static void afterOpenStream(InputStream result, URL url) {
    TrackerRepository.createTracker(result).initDescriptor("InputStream from " + url, null);
  }
}
