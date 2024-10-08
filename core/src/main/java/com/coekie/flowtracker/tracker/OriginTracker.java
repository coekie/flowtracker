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

/** Tracker without a source. Its content cannot change, but it can possibly grow. */
public abstract class OriginTracker extends Tracker {
  private TwinSynchronization twinSync;

  @Override
  public int getEntryCount() {
    return 0;
  }

  @Override
  public boolean isContentMutable() {
    return false;
  }

  @Override
  public void initTwin(Tracker twin) {
    twinSync = new TwinSynchronization(this, twin);
    super.initTwin(twin);
  }

  void beforeAppend() {
    if (twinSync != null) {
      twinSync.beforeAppend(this);
    }
  }

  public TwinSynchronization twinSync() {
    return twinSync;
  }
}
