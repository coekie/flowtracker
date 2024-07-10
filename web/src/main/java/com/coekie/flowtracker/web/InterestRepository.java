package com.coekie.flowtracker.web;

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

import com.coekie.flowtracker.tracker.Tracker;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps track of the trackers that have been returned to the UI */
class InterestRepository {
  private static final ConcurrentHashMap<Long, Tracker> trackers =
      new ConcurrentHashMap<>();

  /**
   * Called when a tracker is returned to the UI, so we can later find it back by id.
   * <p>
   * Note that we don't do this for all trackers as soon as they're created as an optimization, and
   * to limit memory usage.
   */
  static void register(Tracker tracker) {
    trackers.put(tracker.getTrackerId(), tracker);
  }

  static Tracker getContentTracker(long contentTrackerId) {
    return trackers.get(contentTrackerId);
  }
}
