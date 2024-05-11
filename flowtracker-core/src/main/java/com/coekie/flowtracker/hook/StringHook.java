package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.DefaultTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.Trackers;
import com.coekie.flowtracker.util.Config;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class StringHook {
  private static final Field valueField = Reflection.getDeclaredField(String.class, "value");

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
  private static Object getValueArray(String str) {
    return Reflection.getFieldValue(str, valueField);
  }

  @SuppressWarnings({"UnusedDeclaration", "CallToPrintStackTrace"}) // used by instrumented code
  public static void afterInit(String target) {
    if (debugUntracked != null && target.contains(debugUntracked)
        && getStringTracker(target) == null
        // ignore the specifying of the debugUntracked string on the command line itself
        // (but eventually that should be tracked too, see java.lang.ProcessingEnvironment)
        && !target.contains("debugUntracked")) {
      Trackers.suspendOnCurrentThread();
      new Throwable("untracked").printStackTrace();
      Trackers.unsuspendOnCurrentThread();
    }
  }

  /** Get a tracker even when trackers are suspended; to be used from a debugger. */
  @SuppressWarnings("unused")
  public static Tracker forceGetStringTracker(String str) {
    if (Trackers.isActive()) return getStringTracker(str);
    Trackers.unsuspendOnCurrentThread();
    Tracker result = getStringTracker(str);
    Trackers.suspendOnCurrentThread();
    return result;
  }

  // first three arguments are here because this is invoked using ConstantDynamic
  @SuppressWarnings("unused")
  public static String constantString(MethodHandles.Lookup lookup, String name,
      Class<?> type, int classId, int offset, String value) {
    return constantString(value, classId, offset);
  }

  public static String constantString(String value, int classId, int offset) {
    String str = new String(value.getBytes());
    setStringSource(str, ClassOriginTracker.get(classId), offset);
    return str;
  }

  /**
   * Set the source of the array in the string to the given offset in the given tracker. This is for
   * when the creation of the string itself wasn't tracked, but we then after-the-fact add (inject)
   * the tracking of where it came from.
   */
  static void setStringSource(String str, Tracker sourceTracker, int sourceIndex) {
    byte[] valueArray = (byte[]) getValueArray(str);
    DefaultTracker tracker = new DefaultTracker();
    tracker.setSource(0, valueArray.length, sourceTracker, sourceIndex);
    TrackerRepository.forceSetTracker(valueArray, tracker);
  }
}
