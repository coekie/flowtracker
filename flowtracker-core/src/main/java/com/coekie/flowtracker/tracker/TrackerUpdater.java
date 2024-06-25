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

import com.coekie.flowtracker.hook.ByteBufferHook;
import java.nio.ByteBuffer;

/**
 * Helper methods for hooks to update trackers
 */
public class TrackerUpdater {
  public static void setSource(Context context, Object target, int targetIndex, int length,
      Object source, int sourceIndex) {
    setSourceTracker(context, target, targetIndex, length,
        TrackerRepository.getTracker(context, source), sourceIndex, Growth.NONE);
  }

  public static void setSourceTracker(Context context, Object target, int targetIndex, int length,
      Tracker sourceTracker, int sourceIndex) {
    setSourceTracker(context, target, targetIndex, length, sourceTracker, sourceIndex, Growth.NONE);
  }

  public static void setSourceTracker(Context context, Object target, int targetIndex, int length,
      Tracker sourceTracker, int sourceIndex, Growth growth) {
    Tracker targetTracker;
    if (sourceTracker == null) {
      targetTracker = TrackerRepository.getTracker(context, target);
      // if both source and tracker are unknown, do nothing. creating a Tracker just to remember
      // that we do not know where its contents comes from would be useless overhead.
      if (targetTracker == null) return;
    } else {
      targetTracker = TrackerRepository.getOrCreateTracker(context, target);
      // if tracking is disabled, do nothing
      if (targetTracker == null) return;
    }
    targetTracker.setSource(targetIndex, length, sourceTracker, sourceIndex, growth);
  }

  public static void setSourceTrackerPoint(Context context, Object target, int targetIndex,
      int length, TrackerPoint sourcePoint) {
    if (sourcePoint == null) {
      setSourceTracker(context, target, targetIndex, length, null, -1, Growth.NONE);
    } else {
      setSourceTracker(context, target, targetIndex, length, sourcePoint.tracker, sourcePoint.index,
          Growth.of(length, sourcePoint.length));
    }
  }

  public static void appendBytes(Context context, ByteSinkTracker tracker, byte[] src, int offset,
      int length) {
    Tracker sourceTracker = TrackerRepository.getTracker(context, src);
    if (sourceTracker != null) {
      tracker.setSource(tracker.getLength(), length, sourceTracker, offset);
    }
    tracker.append(src, offset, length);
  }

  public static void appendByteBuffer(Context context, ByteSinkTracker tracker, ByteBuffer src,
      int position, int length) {
    if (src.isDirect()) {
      return; // direct buffers are not supported yet
    }

    byte[] hb = ByteBufferHook.hb(src);
    int startOffset = ByteBufferHook.offset(src) + position;
    appendBytes(context, tracker, hb, startOffset, length);
  }
}
