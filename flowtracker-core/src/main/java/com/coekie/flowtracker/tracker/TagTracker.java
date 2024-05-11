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
 * Tracker that associates an object to a Node in the tree. No tracking of content is done.
 * <p>
 * Arguably this class shouldn't actually extend Tracker; but then they'd have to be stored
 * somewhere else than the {@link TrackerRepository}.
 */
public class TagTracker extends Tracker {
  public TagTracker() {
  }

  @Override public int getEntryCount() {
    throw new UnsupportedOperationException();
  }

  @Override public int getLength() {
    throw new UnsupportedOperationException();
  }
}
