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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class TrackerRepository {
  // TODO concurrent weak identity hash map? Use Guava's MapMaker (without pollution classpath)?
  private static final Map<Object, Tracker> objectToTracker =
      Collections.synchronizedMap(new IdentityHashMap<>());

  public static Tracker getTracker(Context context, Object obj) {
    if (!context.isActive()) return null;
    return objectToTracker.get(obj);
  }

  public static Tracker createFixedOriginTracker(Object obj, int length) {
    Tracker tracker = new FixedOriginTracker(length);
    forceSetTracker(obj, tracker);
    return tracker;
  }

  public static Tracker createTracker(Context context, Object obj) {
    Tracker tracker = new DefaultTracker();
    setTracker(context, obj, tracker);
    return tracker;
  }

  public static Tracker getOrCreateTracker(Context context, Object obj) {
    if (!context.isActive()) return null;
    Tracker tracker = objectToTracker.get(obj);
    return tracker == null ? createTracker(context, obj) : tracker;
  }

  public static void setTracker(Context context, Object obj, Tracker tracker) {
    if (obj == null) {
      throw new NullPointerException("Can't track null");
    } else if (getTracker(context, obj) != null) {
      // FIXME race condition: two threads could try to create tracker for same obj at same time.
      //  use Map.putIfAbsent in getOrCreateTracker?
      throw new IllegalStateException("Object already has a tracker: " + obj
          + " has " + getTracker(context, obj));
    } else {
      objectToTracker.put(obj, tracker);
    }
  }

  public static void forceSetTracker(Object obj, Tracker tracker) {
    if (obj == null) {
      throw new NullPointerException("Can't track null");
    } else {
      objectToTracker.put(obj, tracker);
    }
  }

  public static void removeTracker(Object obj) {
    objectToTracker.remove(obj);
  }
}
