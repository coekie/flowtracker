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

import com.coekie.flowtracker.tracker.CharOriginTracker;
import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.DefaultTracker;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.util.Config;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class StringHook {
  private static final VarHandle valueHandle =
      Reflection.varHandle(String.class, "value", byte[].class);
  private static final VarHandle coderHandle =
      Reflection.varHandle(String.class, "coder", byte.class);

  public static final String DEBUG_UNTRACKED = "debugUntracked";

  private static String debugUntracked = null;

  public static void initialize(Config config) {
    debugUntracked = config.get(DEBUG_UNTRACKED);
  }

  public static Tracker getStringTracker(Context context, String str) {
    return TrackerRepository.getTracker(context, getValueArray(str));
  }

  public static void createFakeOriginTracker(String str) {
    TrackerRepository.createFakeOriginTracker(getValueArray(str), str.length());
  }

  public static void removeTracker(String str) {
    TrackerRepository.removeTracker(getValueArray(str));
  }

  /** Get the "value" field from a String */
  private static byte[] getValueArray(String str) {
    return (byte[]) valueHandle.get(str);
  }

  /** Check if a String is latin1 using the "coder" field from a String */
  public static boolean isLatin1(String str) {
    return ((byte) coderHandle.get(str)) == 0;
  }

  @SuppressWarnings({"UnusedDeclaration", "CallToPrintStackTrace"}) // used by instrumented code
  public static void afterInit(String target) {
    if (debugUntracked != null && target.contains(debugUntracked)) {
      Context context = context();
      if (getStringTracker(context, target) == null
        // ignore the specifying of the debugUntracked string on the command line itself
        && !target.contains("debugUntracked")) {
        context.suspend();
        new Throwable("untracked").printStackTrace();
        context.unsuspend();
      }
    }
  }

  /** Get a tracker even when trackers are suspended; to be used from a debugger. */
  @SuppressWarnings("unused")
  public static Tracker forceGetStringTracker(String str) {
    Context context = context();
    if (context.isActive()) return getStringTracker(context, str);
    context.unsuspend();
    Tracker result = getStringTracker(context, str);
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
    Context context = context();
    context.suspend(); // optimization: no point in tracking this
    String str = new String(value.getBytes());
    context.unsuspend();
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

  @SuppressWarnings("unused") // used in CharAtValue
  public static TrackerPoint charAtTracker(String str, int index, Context context) {
    Tracker tracker = getStringTracker(context, str);
    if (tracker == null) {
      return null;
    }
    if (isLatin1(str)) {
      return TrackerPoint.of(tracker, index);
    } else { // non-latin1 Strings store their value as UTF-16, two bytes per character
      return TrackerPoint.of(tracker, index * 2, 2);
    }
  }

  @SuppressWarnings("unused") // used in CharAtValue
  public static TrackerPoint charAtTracker(CharSequence cs, int index, Context context) {
    if (cs instanceof String) {
      return charAtTracker((String) cs, index, context);
    } else {
      // this is a bit behaviour-changing, but we call charAt a second time to get the TrackerPoint
      Invocation invocation = Invocation.create("charAt (I)C").calling(context);
      //noinspection ResultOfMethodCallIgnored
      cs.charAt(index);
      return invocation.returnPoint;
    }
  }

  public static void ensureInitialized() {
    getValueArray("");
    StringHook.isLatin1("");
  }

  /**
   * Tests if String/StringUTF16 was correctly instrumented.
   * <p>
   * The goal here is to catch situations where instrumentation silently failed for some reason.
   * This is kinda a regression test for a problem that only happened when running with the real
   * (not dev) agent, so could not be tested in a usual unit test; where StringUTF16 wasn't getting
   * instrumented, which was fixed by adding a second round of instrumentation in WeaverInitializer.
   */
  public static void selfTest() {
    Context context = context();
    char[] chars = new char[10];
    CharOriginTracker tracker = new CharOriginTracker();
    tracker.append("          ");
    TrackerRepository.setTracker(context, chars, tracker);
    String str = new String(chars, 1, 5);
    if (StringHook.getStringTracker(context, str) == null) {
      throw new Error("FlowTracker self-test failed. Failed to instrument String classes?");
    }
  }
}
