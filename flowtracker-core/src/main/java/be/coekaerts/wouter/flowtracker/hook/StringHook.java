package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.lang.reflect.Field;

public class StringHook {
  private static final Field valueField = Reflection.getDeclaredField(String.class, "value");

  public static final String DEBUG_UNTRACKED = "debugUntracked";

  private static String debugUntracked = null;

  public static void initDebugUntracked(String debugUntrackedConfig) {
    debugUntracked = debugUntrackedConfig;
  }

  public static Tracker getStringTracker(String str) {
    return TrackerRepository.getTracker(getValueArray(str));
  }

  @SuppressWarnings("unused") // used by instrumented code
  public static Tracker getCharSequenceTracker(CharSequence cs) {
    // for now we only support Strings (when we support more this should move somewhere else)
    return cs instanceof String ? getStringTracker((String) cs) : null;
  }

  public static void createFixedOriginTracker(String str) {
    TrackerRepository.createFixedOriginTracker(getValueArray(str), str.length());
  }

  /** Get the "value" field from a String */
  private static Object getValueArray(String str) {
    return Reflection.getFieldValue(str, valueField);
  }

  @SuppressWarnings("UnusedDeclaration") // used by instrumented code
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
}
