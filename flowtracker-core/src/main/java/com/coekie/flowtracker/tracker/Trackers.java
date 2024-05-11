package com.coekie.flowtracker.tracker;

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
