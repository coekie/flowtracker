package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Map;

public abstract class Tracker {

	Tracker() {
	}
	
	/**
	 * Set a range of the source of this tracker to the given tracker.
	 * 
	 * If <tt>sourceTracker</tt> is not appropriate as a direct source for this tracker
	 * (e.g. because it is not immutable, or is not the original source), this may instead use the source
	 * of the source (and recurse) using pushContentToTracker on <tt>sourceTracker</tt>.
	 *  
	 * @param index Index in this tracker
	 * @param length Size of the range
	 * @param sourceTracker Tracker to use as source
	 * @param sourceIndex Index into <tt>sourceTracker</tt>
	 */
	abstract void setSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex);
	
	public abstract int getEntryCount();
	
	/**
	 * Returns the entry in the tracker for the given index.
	 * 
	 * The key of the given entry is the index in this tracker where the entry begins.
	 * This may be equal to the given <tt>index</tt>, or less (if {@link PartTracker#getLength()} > 1).
	 * 
	 * @param index The index in this Tracker
	 */
	public Map.Entry<Integer, PartTracker> getEntryAt(int index) {
		throw new UnsupportedOperationException();
	}
	
	public int getStartIndexAt(int index) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * A tracker is mutable if {@link #isContentMutable()} or {@link #canGrow()}.
	 */
	public final boolean isMutable() {
		return isContentMutable() || canGrow();
	}
	
	/**
	 * Returns if the length of this tracker can become bigger.
	 */
	public boolean canGrow() {
		return true;
	}
	
	/**
	 * Returns if the existing content of this tracker can be changed.
	 * Extra content added at the end does not count.
	 * Anything added or changed in the beginning or middle does count.
	 * <p>
	 * In other words, if this method returns true, the source of anything between index 0 and the current
	 * {@link #getLength()} is not allowed to change.
	 * <p>
	 * A tracker can only be considered immutable if the trackers it stores as source are also immutable.
	 */
	public boolean isContentMutable() {
		return true;
	}
	
	/**
	 * Returns the length of the content of the object this Tracker is tracking.
	 * Note that this is not necessarily equal to the last known index in this tracker,
	 * because there may be unkown content at the end (which is included in this length).
	 */
	public abstract int getLength();
	
	/**
	 * Put a range of the content of this tracker into the given target tracker.
	 * This should be implemented by calling setSource on the target tracker.
	 * Note that it is not <em>this</em> tracker that should be pushed, but the content (the source) of this tracker.
	 * 
	 * @param sourceIndex Index in this tracker of where the range starts.
	 * @param length Size of the range
	 * @param targetTracker Tracker of which we're setting the source to this one
	 * @param targetIndex Offset in <tt>targetTracker</tt> of where the range starts.
	 */
	public void pushContentToTracker(int sourceIndex, int length, Tracker targetTracker, int targetIndex) {
		throw new UnsupportedOperationException();
	}
	
	public static void setSource(Object target, int targetIndex, int length, Object source, int sourceIndex) {
		setSourceTracker(target, targetIndex, length, TrackerRepository.getTracker(source), sourceIndex);
	}

	public static void setSourceTracker(Object target, int targetIndex, int length, Tracker sourceTracker, int sourceIndex) {
    if (!Trackers.isActive()) return;

		if (sourceTracker == null) {
			// if we're not tracking the source, we don't care where its content goes to
			// TODO if targetTracker already has info for that range, erase it!
			return;
		}
		
		Tracker targetTracker = TrackerRepository.getOrCreateTracker(target);
		targetTracker.setSourceFromTracker(targetIndex, length, sourceTracker, sourceIndex);
	}
}
