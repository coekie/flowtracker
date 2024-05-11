package com.coekie.flowtracker.tracker;

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

/**
 * Helper methods for hooks to update trackers
 */
public class TrackerUpdater {
  public static void setSource(Object target, int targetIndex, int length, Object source,
      int sourceIndex) {
    setSourceTracker(target, targetIndex, length, TrackerRepository.getTracker(source),
        sourceIndex);
  }

  public static void setSourceTracker(Object target, int targetIndex, int length,
      Tracker sourceTracker, int sourceIndex) {
    setSourceTracker(target, targetIndex, length, sourceTracker, sourceIndex, Growth.NONE);
  }

  public static void setSourceTracker(Object target, int targetIndex, int length,
      Tracker sourceTracker, int sourceIndex, Growth growth) {
    Tracker targetTracker;
    if (sourceTracker == null) {
      targetTracker = TrackerRepository.getTracker(target);
      // unknown source and unknown target; nothing to do
      if (targetTracker == null) return;
    } else {
      targetTracker = TrackerRepository.getOrCreateTracker(target);
      // if tracking is disabled, do nothing
      if (targetTracker == null) return;
    }
    targetTracker.setSource(targetIndex, length, sourceTracker, sourceIndex, growth);
  }

  public static void setSourceTrackerPoint(Object target, int targetIndex, int length,
      TrackerPoint sourcePoint) {
    if (sourcePoint == null) {
      setSourceTracker(target, targetIndex, length, null, -1);
    } else {
      setSourceTracker(target, targetIndex, length, sourcePoint.tracker, sourcePoint.index,
          Growth.of(length, sourcePoint.length));
    }
  }
}
