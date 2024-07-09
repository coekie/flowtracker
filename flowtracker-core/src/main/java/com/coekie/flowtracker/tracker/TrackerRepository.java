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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.util.ConcurrentWeakIdentityHashMap;

/**
 * Holds the mapping from objects to their {@link Tracker}s.
 */
public class TrackerRepository {
  private static final ConcurrentWeakIdentityHashMap<Object, Tracker> objectToTracker =
      new ConcurrentWeakIdentityHashMap<>();

  /** Get the tracker for `obj`, or null if it doesn't have one or tracking is disabled. */
  public static Tracker getTracker(Context context, Object obj) {
    if (!context.isActive()) return null;
    return forceGetTracker(context, obj);
  }

  /**
   * Return the tracker for `obj`, or create a {@link DefaultTracker} if it doesn't have one yet.
   */
  public static Tracker getOrCreateTracker(Context context, Object obj) {
    if (!context.isActive()) return null;
    Tracker existingTracker = forceGetTracker(context, obj);
    if (existingTracker != null) {
      return existingTracker;
    }
    Tracker newTracker = new DefaultTracker();
    Tracker existingTracker2 = objectToTracker.putIfAbsent(obj, newTracker);
    if (existingTracker2 != null) { // race condition, tracker created concurrently
      return existingTracker2;
    }
    overwriteCachedTracker(context, obj, newTracker);
    return newTracker;
  }

  /** Sets the tracker for `obj`. Should only be called once; throws if it already has a tracker */
  public static void setTracker(Context context, Object obj, Tracker tracker) {
    if (obj == null) {
      throw new NullPointerException("Can't track null");
    } else {
      Tracker existingTracker = objectToTracker.putIfAbsent(obj, tracker);
      if (existingTracker != null) {
        throw new IllegalStateException("Object already has a tracker: " + obj
            + " has " + getTracker(context, obj));
      }
      overwriteCachedTracker(context, obj, tracker);
    }
  }

  /**
   * Overwrites the tracker for `obj`.
   * We don't usually change the tracker associated to an object; this should be used rarely.
   */
  public static void forceSetTracker(Object obj, Tracker tracker) {
    if (obj == null) {
      throw new NullPointerException("Can't track null");
    } else {
      objectToTracker.put(obj, tracker);
      overwriteCachedTracker(context(), obj, tracker);
    }
  }

  public static void removeTracker(Object obj) {
    objectToTracker.remove(obj);
    overwriteCachedTracker(context(), obj, null);
  }

  /** Set a {@link FakeOriginTracker} for `obj`, for testing */
  public static Tracker createFakeOriginTracker(Object obj, int length) {
    Tracker tracker = new FakeOriginTracker(length);
    forceSetTracker(obj, tracker);
    return tracker;
  }

  /**
   * Get the tracker for obj, without checking if tracking is active.
   */
  // For the cache, we store recently queried objects in the Context, with the assumption that the
  // same objects are often queried repeatedly in the same thread.
  // It's a very small cache, just to avoid querying {@link #objectToTracker}.
  private static Tracker forceGetTracker(Context context, Object obj) {
    if (obj == null) {
      return null;
    } else if (context.cachedObj0 == obj) {
      return context.cachedTracker0;
    } else if (context.cachedObj1 == obj) {
      return context.cachedTracker1;
    } else if (context.cachedObj2 == obj) {
      return context.cachedTracker2;
    } else { // cache miss
      Tracker result = objectToTracker.get(obj);
      // store it in the cache
      if (context.nextCache == 0) {
        context.cachedObj0 = obj;
        context.cachedTracker0 = result;
        context.nextCache = 1;
      } else if (context.nextCache == 1) {
        context.cachedObj1 = obj;
        context.cachedTracker1 = result;
        context.nextCache = 2;
      } else {
        context.cachedObj2 = obj;
        context.cachedTracker2 = result;
        context.nextCache = 0;
      }
      return result;
    }
  }

  // overwrite existing cache entry, if any.
  // we assume that the setting of a tracker happens in the thread that created it, before it
  // escapes; so it's not necessary to clear the caches of any other threads.
  private static void overwriteCachedTracker(Context context, Object obj, Tracker tracker) {
    if (context.cachedObj0 == obj) {
      context.cachedTracker0 = tracker;
    } else if (context.cachedObj1 == obj) {
      context.cachedTracker1 = tracker;
    } else if (context.cachedObj2 == obj) {
      context.cachedTracker2 = tracker;
    }
  }
}
