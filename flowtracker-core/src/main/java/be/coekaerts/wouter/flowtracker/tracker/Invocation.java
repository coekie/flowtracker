package be.coekaerts.wouter.flowtracker.tracker;

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

  // tracker and index for the primitive value in the first argument
  public Tracker arg0Tracker;
  public int arg0Index;

  Invocation(String signature) {
    this.signature = signature;
    this.arg0Tracker = null;
    this.arg0Index = -1;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public TrackerPoint getReturnPoint() {
    return TrackerPoint.ofNullable(returnTracker, returnIndex);
  }

  public Invocation setArg0(Tracker arg0Tracker, int arg0Index) {
    this.arg0Tracker = arg0Tracker;
    this.arg0Index = arg0Index;
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
