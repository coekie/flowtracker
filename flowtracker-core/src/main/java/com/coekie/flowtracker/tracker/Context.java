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

import java.util.function.Consumer;

/**
 * Keeps the state of flowtracker for a particular thread.
 * <p>
 * Alternatively we could have made the fields in here separate ThreadLocals, each managed by their
 * own classes (e.g. `pendingInvocation` being a ThreadLocal in `Invocation`), but that requires
 * accessing ThreadLocal more often, which has a significant performance impact.
 */
public class Context implements Runnable {
  private static final ContextSupplier supplier = ContextSupplier.initSupplier();

  /** Get or create the Context of the current thread */
  public static Context context() {
    return supplier.get();
  }

  @Override
  public void run() {
    // the fact that this class implements Runnable is a weird hack needed because of
    // ContextSupplier.WithThreadTarget
    throw new UnsupportedOperationException();
  }

  /** Currently pending invocation. Should only be used by the {@link Invocation} implementation */
  Invocation pendingInvocation;

  /** Number of times this thread has (recursively) been suspended */
  int suspended;

  // cache for TrackerRepository
  Object cachedObj0;
  Tracker cachedTracker0;
  Object cachedObj1;
  Tracker cachedTracker1;
  Object cachedObj2;
  Tracker cachedTracker2;
  int nextCache;

  /** When non-null this gets called for every class that is being transformed by our agent */
  public Consumer<String> transformListener;

  /** Checks if tracking is currently active on this thread */
  public boolean isActive() {
    // uncommentable hack to fix debugging after a while if tracking completely breaks things
    //private static long startTime = System.currentTimeMillis();
    //if (System.currentTimeMillis() - startTime > 3000) return false;

    return suspended == 0;
  }

  /** Disable tracking on this thread. See {@link #isActive()}, {@link #unsuspend()}. */
  public void suspend() {
    suspended++;
  }

  /** Re-enable tracking on this thread. See {@link #isActive()}, {@link #suspend()}. */
  public void unsuspend() {
    if (suspended == 0) {
      throw new IllegalStateException("not suspended");
    }
    suspended--;
  }
}
