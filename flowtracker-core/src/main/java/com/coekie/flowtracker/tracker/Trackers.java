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

public class Trackers {
  private static final ThreadLocal<Integer> suspended = new ThreadLocal<>();

  //private static long startTime = System.currentTimeMillis();

  /** Checks if tracking is currently active on this thread */
  public static boolean isActive() {
    // uncommentable hack to fix debugging after a while if tracking completely breaks things
    //if (System.currentTimeMillis() - startTime > 3000) return false;

    return suspended.get() == null;
  }

  public static void suspendOnCurrentThread() {
    Integer currentSuspended = suspended.get();
    suspended.set(currentSuspended == null ? 1 : currentSuspended + 1);
  }

  public static void unsuspendOnCurrentThread() {
    Integer currentSuspended = suspended.get();
    if (currentSuspended == null) {
      throw new IllegalStateException("not suspended");
    }
    if (currentSuspended == 1) {
      suspended.remove();
    } else {
      suspended.set(currentSuspended - 1);
    }
  }
}
