package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.tracker.TagTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.Trackers;
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
