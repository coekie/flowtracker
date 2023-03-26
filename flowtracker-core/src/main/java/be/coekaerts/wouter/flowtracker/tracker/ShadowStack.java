package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Facilitates tracking of primitive values through calls, for return values and parameters.
 */
public class ShadowStack {
  private static final ThreadLocal<Invocation> pending = new ThreadLocal<>();

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

  /** Represents a method call, to both the caller and callee */
  public static class Invocation {
    private final String desc;
    public Tracker returnTracker;
    public int returnIndex;

    private Invocation(String desc) {
      this.desc = desc;
    }

    /** Sets the source tracker of a returned value */
    public static void returning(Invocation invocation, Tracker tracker, int index) {
      if (invocation != null) {
        invocation.returnTracker = tracker;
        invocation.returnIndex = index;
      }
    }
  }
}
