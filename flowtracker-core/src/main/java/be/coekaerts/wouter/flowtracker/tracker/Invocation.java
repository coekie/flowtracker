package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Represents a method call, to both the caller and callee.
 * Facilitates tracking of primitive values through calls, for return values and parameters.
 */
public class Invocation {
  private static final ThreadLocal<Invocation> pending = new ThreadLocal<>();

  private final String signature;
  public Tracker returnTracker;
  public int returnIndex;

  Invocation(String signature) {
    this.signature = signature;
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
