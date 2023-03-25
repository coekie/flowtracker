package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Facilitates tracking of primitive values through calls, for return values and parameters.
 */
public class ShadowStack {
  private static final ThreadLocal<Frame> pending = new ThreadLocal<>();

  /**
   * Called by a caller before calling another method through which we want to track return or
   * parameter values.
   */
  public static Frame calling(String desc) {
    Frame frame = new Frame(desc);
    pending.set(frame);
    return frame;
  }

  /**
   * Called inside the called method
   */
  public static Frame start(String desc) {
    Frame frame = pending.get();
    if (frame != null) {
      pending.remove();
      if (desc.equals(frame.desc)) {
        return frame;
      }
    }
    return null;
  }

  /** Represents a method call, to both the caller and callee */
  public static class Frame {
    private final String desc;
    public Tracker returnTracker;
    public int returnIndex;

    private Frame(String desc) {
      this.desc = desc;
    }

    /** Sets the source tracker of a returned value */
    public static void returning(Frame frame, Tracker tracker, int index) {
      if (frame != null) {
        frame.returnTracker = tracker;
        frame.returnIndex = index;
      }
    }
  }
}
