package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Represents a method call, to both the caller and callee.
 * Facilitates tracking of primitive values through calls, for return values and parameters.
 */
public class Invocation {
  private static final ThreadLocal<Invocation> pending = new ThreadLocal<>();

  private final String desc;
  public Tracker returnTracker;
  public int returnIndex;

  Invocation(String desc) {
    this.desc = desc;
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

  /**
   * Called by a caller before calling another method through which we want to track return or
   * parameter values.
   */
  public static Invocation calling(String desc) {
    Invocation invocation = new Invocation(desc);
    pending.set(invocation);
    return invocation;
  }

  /**
   * Called inside the called method
   */
  public static Invocation start(String desc) {
    Invocation invocation = pending.get();
    if (invocation != null) {
      pending.remove();
      if (desc.equals(invocation.desc)) {
        return invocation;
      }
    }
    return null;
  }
}
