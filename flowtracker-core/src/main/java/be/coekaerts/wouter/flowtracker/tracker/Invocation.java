package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Arrays;

/**
 * Represents a method call, to both the caller and callee.
 * Facilitates tracking of primitive values through calls, for return values and parameters.
 */
public class Invocation {
  private static final ThreadLocal<Invocation> pending = new ThreadLocal<>();

  private final String signature;

  // tracker and index for the returned primitive value
  public Tracker returnTracker;
  public int returnIndex;

  // tracks source for some primitive values in arguments. null for untracked arguments.
  private TrackerPoint[] args;

  Invocation(String signature) {
    this.signature = signature;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public TrackerPoint getReturnPoint() {
    return TrackerPoint.ofNullable(returnTracker, returnIndex);
  }

  // invoked by instrumentation
  public Invocation setArg(int argNum, TrackerPoint trackerPoint) {
    if (trackerPoint == null) {
      return this;
    }

    if (args == null) {
      args = new TrackerPoint[argNum + 1];
    } else if (args.length < argNum + 1) {
      // alternative: take the number of arguments as constructor parameter
      args = Arrays.copyOf(args, Math.max(argNum + 1, args.length * 2));
    }
    args[argNum] = trackerPoint;

    return this;
  }

  /**
   * Sets the source tracker of a returned value
   */
  public static void returning(Invocation invocation, Tracker tracker, int index) {
    if (invocation != null) {
      invocation.returnTracker = tracker;
      invocation.returnIndex = index;
    }
  }

  public static Tracker getArgTracker(Invocation invocation, int argNum) {
    TrackerPoint trackerPoint = getArgPoint(invocation, argNum);
    return trackerPoint == null ? null : trackerPoint.tracker;
  }

  public static int getArgIndex(Invocation invocation, int argNum) {
    TrackerPoint trackerPoint = getArgPoint(invocation, argNum);
    return trackerPoint == null ? -1 : trackerPoint.index;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static Tracker getArg0Tracker(Invocation invocation) {
    return getArgTracker(invocation, 0);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static int getArg0Index(Invocation invocation) {
    return getArgIndex(invocation, 0);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static Tracker getArg1Tracker(Invocation invocation) {
    return getArgTracker(invocation, 1);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static int getArg1Index(Invocation invocation) {
    return getArgIndex(invocation, 1);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static Tracker getArg2Tracker(Invocation invocation) {
    return getArgTracker(invocation, 2);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static int getArg2Index(Invocation invocation) {
    return getArgIndex(invocation, 2);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static Tracker getArg3Tracker(Invocation invocation) {
    return getArgTracker(invocation, 3);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static int getArg3Index(Invocation invocation) {
    return getArgIndex(invocation, 3);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static Tracker getArg4Tracker(Invocation invocation) {
    return getArgTracker(invocation, 4);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static int getArg4Index(Invocation invocation) {
    return getArgIndex(invocation, 4);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static Tracker getArg5Tracker(Invocation invocation) {
    return getArgTracker(invocation, 5);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public static int getArg5Index(Invocation invocation) {
    return getArgIndex(invocation, 5);
  }

  // invoked by instrumentation
  public static TrackerPoint getArgPoint(Invocation invocation, int argNum) {
    if (invocation == null) {
      return null;
    }
    TrackerPoint[] args = invocation.args;
    return args != null && args.length > argNum ? args[argNum] : null;
  }

  /**
   * Called by a caller before calling another method through which we want to track return or
   * parameter values.
   */
  public static Invocation calling(String signature) {
    Invocation invocation = new Invocation(signature);
    pending.set(invocation);
    return invocation;
  }

  /**
   * Called inside the called method
   */
  public static Invocation start(String signature) {
    Invocation invocation = pending.get();
    if (invocation != null) {
      pending.remove();
      if (signature.equals(invocation.signature)) {
        return invocation;
      }
    }
    return null;
  }

  public static String signature(String name, String desc) {
    return name + " " + desc;
  }
}
