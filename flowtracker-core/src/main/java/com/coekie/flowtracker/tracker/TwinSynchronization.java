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

import java.util.ArrayList;
import java.util.List;

/**
 * For two twins ({@link Tracker#twin()}), records corresponding positions, so that their content
 * can be interleaved. So if their content is like a conversation, you can see what part is a
 * response to what part in the other side.
 */
public class TwinSynchronization {
  private final Tracker tracker1;
  private final Tracker tracker2;
  public final List<TwinMarker> markers = new ArrayList<>();
  private Tracker prevAppend;

  TwinSynchronization(Tracker tracker1, Tracker tracker2) {
    this.tracker1 = tracker1;
    this.tracker2 = tracker2;
  }

  void beforeAppend(Tracker tracker) {
    if (prevAppend != null && prevAppend != tracker) {
      markers.add(new TwinMarker(tracker, tracker.getLength(), other(tracker).getLength()));

    }
    prevAppend = tracker;
  }

  /** The other tracker; assuming tracker is one of {@link #tracker1} or {@link #tracker2} */
  public Tracker other(Tracker tracker) {
    return tracker1 == tracker ? tracker2 : tracker1;
  }

  /**
   * Marks the position at which the conversation switched sides; that is when content got added to
   * a different tracker than before.
   */
  public class TwinMarker {
    /** The tracker which started getting content at this point */
    public final Tracker to;

    /** The position in the {@link #to} tracker */
    public final int toIndex;

    /** The corresponding position in the other tracker */
    public final int fromIndex;

    private TwinMarker(Tracker to, int toIndex, int fromIndex) {
      this.to = to;
      this.toIndex = toIndex;
      this.fromIndex = fromIndex;
    }

    /** The other tracker */
    public Tracker from() {
      return other(to);
    }
  }
}
