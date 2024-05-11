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
import java.util.List;

/** Reference to a Tracker */
@SuppressWarnings("UnusedDeclaration") // json
public class TrackerResponse {
  public final long id;
  public final List<String> path;
  public final boolean origin;
  public final boolean sink;

  TrackerResponse(Tracker tracker) {
    this.id = tracker.getTrackerId();
    this.path = tracker.getNode() == null ? null : tracker.getNode().path();
    this.origin = TrackerResource.isOrigin(tracker);
    this.sink = TrackerResource.isSink(tracker);
    InterestRepository.register(tracker);
  }
}
