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

/** Specifies how deep to look for the source of a value. */
// TODO the only depths we have are CONTENT_IMMUTABLE and ORIGIN, and in practice they are the same.
// While e.g. String is immutable, we are just tracking the content of it which is an array which as
// far as we know is mutable. If we wanted to be able to see intermediate Strings that something
// went through, perhaps we could tag such arrays as effectively immutable.
// Not sure if that's actually useful.
public interface TrackerDepth {
  /** Find an immutable tracker. */
  public static final TrackerDepth CONTENT_IMMUTABLE = new TrackerDepth() {
    @Override
    public boolean isAcceptableContent(Tracker sourceTracker) {
      return !sourceTracker.isContentMutable();
    }
  };

  /** Go all the way to the origin. */
  public static final TrackerDepth ORIGIN = new TrackerDepth() {
    @Override
    public boolean isAcceptableContent(Tracker sourceTracker) {
      return sourceTracker.getEntryCount() == 0;
    }
  };

  /**
   * Returns if the specified sourceTracker can be part of this tracker.
   * If not, we have to go deeper: the content of sourceTracker should be used instead.
   */
  boolean isAcceptableContent(Tracker sourceTracker);
}
