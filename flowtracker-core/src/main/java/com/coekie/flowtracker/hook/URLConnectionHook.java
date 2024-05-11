package com.coekie.flowtracker.hook;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
