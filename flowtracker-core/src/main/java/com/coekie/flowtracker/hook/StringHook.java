package com.coekie.flowtracker.hook;

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

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.DefaultTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.util.Config;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class StringHook {
  private static final MethodHandle valueGetter =
      Reflection.getter(String.class, "value", byte[].class);

  public static final String DEBUG_UNTRACKED = "debugUntracked";

  private static String debugUntracked = null;

  public static void initialize(Config config) {
    debugUntracked = config.get(DEBUG_UNTRACKED);
  }

  public static Tracker getStringTracker(String str) {
    return TrackerRepository.getTracker(getValueArray(str));
  }

  public static void createFixedOriginTracker(String str) {
    TrackerRepository.createFixedOriginTracker(getValueArray(str), str.length());
  }

  public static void removeTracker(String str) {
    TrackerRepository.removeTracker(getValueArray(str));
  }

  /** Get the "value" field from a String */
  private static byte[] getValueArray(String str) {
    try {
      return (byte[]) valueGetter.invokeExact(str);
    } catch (Throwable t) {
      throw new Error(t);
    }
  }

  @SuppressWarnings({"UnusedDeclaration", "CallToPrintStackTrace"}) // used by instrumented code
  public static void afterInit(String target) {
    if (debugUntracked != null && target.contains(debugUntracked)
        && getStringTracker(target) == null
        // ignore the specifying of the debugUntracked string on the command line itself
        // (but eventually that should be tracked too, see java.lang.ProcessingEnvironment)
        && !target.contains("debugUntracked")) {
      Context context = context();
      context.suspend();
      new Throwable("untracked").printStackTrace();
      context.unsuspend();
    }
  }

  /** Get a tracker even when trackers are suspended; to be used from a debugger. */
  @SuppressWarnings("unused")
  public static Tracker forceGetStringTracker(String str) {
    Context context = context();
    if (context.isActive()) return getStringTracker(str);
    context.unsuspend();
    Tracker result = getStringTracker(str);
    context.suspend();
    return result;
  }

  // first three arguments are here because this is invoked using ConstantDynamic
  @SuppressWarnings("unused")
  public static String constantString(MethodHandles.Lookup lookup, String name,
      Class<?> type, int classId, int offset, String value) {
    return constantString(value, classId, offset);
  }

  public static String constantString(String value, int classId, int offset) {
    return constantString(value, ClassOriginTracker.get(classId), offset);
  }

  public static String constantString(String value, ClassOriginTracker tracker, int offset) {
    String str = new String(value.getBytes());
    setStringSource(str, tracker, offset);
    return str;
  }

  /**
   * Set the source of the array in the string to the given offset in the given tracker. This is for
   * when the creation of the string itself wasn't tracked, but we then after-the-fact add (inject)
   * the tracking of where it came from.
   */
  static void setStringSource(String str, Tracker sourceTracker, int sourceIndex) {
    byte[] valueArray = getValueArray(str);
    DefaultTracker tracker = new DefaultTracker();
    tracker.setSource(0, valueArray.length, sourceTracker, sourceIndex);
    TrackerRepository.forceSetTracker(valueArray, tracker);
  }
}
