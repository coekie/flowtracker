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
 * Keeps the state of flowtracker for a particular thread.
 * <p>
 * Alternatively we could have made the fields in here separate ThreadLocals, each managed by their
 * own classes (e.g. `pendingInvocation` being a ThreadLocal in `Invocation`), but that requires
 * accessing ThreadLocal more often, which has a significant performance impact.
 */
public class Context {
  private static final ThreadLocal<Context> threadLocal = ThreadLocal.withInitial(Context::new);

  public static Context context() {
    return threadLocal.get();
  }

  /** Currently pending invocation. Should only be used by the {@link Invocation} implementation */
  Invocation pendingInvocation;

  /** Number of times this thread has (recursively) been suspended */
  int suspended;

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
