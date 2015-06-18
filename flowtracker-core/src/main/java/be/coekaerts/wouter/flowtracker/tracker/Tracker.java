package be.coekaerts.wouter.flowtracker.tracker;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Tracker implements WritableTracker {
  private static final AtomicLong idGenerator = new AtomicLong();
  private final long trackerId = idGenerator.getAndIncrement();

  private String descriptor;
  private Tracker descriptorTracker;

  Tracker() {
  }

  public long getTrackerId() {
    return trackerId;
  }

  @Override public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex) {
    throw new UnsupportedOperationException();
  }

  public abstract int getEntryCount();

  /**
   * Returns if the existing content of this tracker can be changed.
   * Extra content added at the end does not count.
   * Anything added or changed in the beginning or middle does count.
   * <p>
   * In other words, if this method returns true, the source of anything between index 0 and the
   * current {@link #getLength()} is not allowed to change.
   * <p>
   * A tracker can only be considered immutable if the trackers it stores as source are also
   * immutable.
   */
  public boolean isContentMutable() {
    return true;
  }

  /**
   * Returns the length of the content of the object this Tracker is tracking.
   * Note that this is not necessarily equal to the last known index in this tracker,
   * because there may be unknown content at the end (which is included in this length).
   */
  public abstract int getLength();

  /**
   * Put a range of the source of this tracker into the given target tracker.
   * This should be implemented by calling {@link WritableTracker#setSource} on the target,
   * possibly multiple times.
   * Note that it is not <em>this</em> tracker that should be pushed, but the source of this.
   *
   * @param sourceIndex Index in this tracker of where the range starts.
   * @param length Size of the range
   * @param targetTracker Tracker of which we're setting the source to this one
   * @param targetIndex Offset in <tt>targetTracker</tt> of where the range starts.
   */
  public void pushSourceTo(int sourceIndex, int length, WritableTracker targetTracker,
      int targetIndex) {
    throw new UnsupportedOperationException();
  }

  /** Initializes {@link #getDescriptor()} and {@link #getDescriptorTracker()} */
  public void initDescriptor(String descriptor, Tracker descriptorTracker) {
    if (this.descriptor != null) {
      throw new IllegalStateException("Descriptor already initialized: " + this.descriptor);
    }
    this.descriptor = descriptor;
    this.descriptorTracker = descriptorTracker;
  }

  /** Description of what kind of object is being tracked and/or where it got created from */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * Tracker of object used to created the target of this tracker. E.g. if this is a tracker for
   * an InputStreamReader, the tracker for the InputStream is returned
   */
  public Tracker getDescriptorTracker() {
    return descriptorTracker;
  }

  /** if this tracker has content; if {@link #getContent()} is supported */
  public boolean supportsContent() {
    return false;
  }

  /**
   * The current content for this tracker, or throws {@link UnsupportedOperationException} if not
   * {@link #supportsContent()}.
   */
  public CharSequence getContent() {
    throw new UnsupportedOperationException();
  }
}
