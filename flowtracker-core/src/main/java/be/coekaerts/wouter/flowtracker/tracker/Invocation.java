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
  public TrackerPoint returnPoint;

  // tracks source for some primitive values in arguments. null for untracked arguments.
  private TrackerPoint[] args;

  Invocation(String signature) {
    this.signature = signature;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Tracker getReturnTracker() {
    return returnPoint == null ? null : returnPoint.tracker;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public int getReturnIndex() {
    return returnPoint == null ? -1 : returnPoint.index;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg0(TrackerPoint trackerPoint) {
    return setArg(0, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg1(TrackerPoint trackerPoint) {
    return setArg(1, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg2(TrackerPoint trackerPoint) {
    return setArg(2, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg3(TrackerPoint trackerPoint) {
    return setArg(3, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg4(TrackerPoint trackerPoint) {
    return setArg(4, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg5(TrackerPoint trackerPoint) {
    return setArg(5, trackerPoint);
  }

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
   * Called by a caller just before calling another method through which we want to track return or
   * parameter values.
   */
  public Invocation calling() {
    pending.set(this);
    return this;
  }

  /**
   * Sets the source tracker of a returned value
   */
  public static void returning(Invocation invocation, TrackerPoint returnPoint) {
    if (invocation != null) {
      invocation.returnPoint = returnPoint;
    }
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
   * Creates an {@link Invocation}. Can optionally be followed by calls to
   * {@link #setArg0(TrackerPoint)} and friends, and should be followed by {@link #calling()} before
   * doing the actual call.
   */
  public static Invocation create(String signature) {
    return new Invocation(signature);
  }

  /**
   * Called by a caller before calling another method through which we want to track return or
   * parameter values.
   */
  public static Invocation createCalling(String signature) {
    return create(signature).calling();
  }

  /**
   * Called inside the called method
   */
  public static Invocation start(String signature) {
    Invocation invocation = pending.get();
    if (invocation != null) {
      clear();
      if (signature.equals(invocation.signature)) {
        return invocation;
      }
    }
    return null;
  }

  public static String signature(String name, String desc) {
    return name + " " + desc;
  }

  /**
   * Removes the current pending Invocation from the thread-local, to be restored later with
   * {@link #unsuspend(Invocation)}.
   * <p>
   * This is used to solve a problem caused by class loading and initialization triggered by a
   * method invocation.
   * The problem is that the loading and initialization can happen between when the caller calls
   * {@link #calling()} and when the callee calls {@link #start(String)}. And it may involve
   * other method calls that use Invocation. Since we only keep track of one pending call, this
   * means class loading would make us forget about the pending call.
   * We solve that by, when class loading is triggered, removing the current pending call, and
   * restoring it when class loading has finished.
   */
  public static Invocation suspend() {
    Invocation invocation = pending.get();
    if (invocation != null) {
      clear();
    }
    return invocation;
  }

  /** @see #suspend() */
  public static void unsuspend(Invocation invocation) {
    pending.set(invocation);
  }

  // deliberately not using ThreadLocal.clear(), because constantly adding and removing from the
  // map is slower than updating an existing entry.
  @SuppressWarnings("ThreadLocalSetWithNull")
  private static void clear() {
    pending.set(null);
  }

  public static Invocation peekPending() {
    return pending.get();
  }
}
