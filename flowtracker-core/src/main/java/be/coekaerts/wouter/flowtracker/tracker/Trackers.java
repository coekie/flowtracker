package be.coekaerts.wouter.flowtracker.tracker;

public class Trackers {
  private static ThreadLocal<Integer> suspended = new ThreadLocal<Integer>();

  /** Checks if tracking is currently active on this thread */
  public static boolean isActive() {
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
