package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TagTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.net.URLConnection;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class URLConnectionHook {
  public static void afterGetInputStream(InputStream result, URLConnection connection) {
    if (!Trackers.isActive()) return;
    Tracker tracker = InputStreamHook.getInputStreamTracker(result);
    // (since ZipFile is hooked, and we haven't added tests for any other URLConnection, this code
    // is untested)
    if (tracker == null) {
      tracker = new TagTracker();
      TrackerRepository.setTracker(result, tracker);
    }
    if (tracker.getNode() == null) {
      tracker.addTo(TrackerTree.ROOT.node("URLConnection")
          .pathNode(connection.getURL().toString()));
    }
  }
}
