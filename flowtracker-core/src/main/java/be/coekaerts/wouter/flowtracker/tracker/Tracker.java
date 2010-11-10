package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Map;

public abstract class Tracker {

	Tracker() {
	}
	
	abstract void setSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex);
	
	public abstract int getEntryCount();
	
	/**
	 * Returns the entry in the tracker for the given index.
	 * 
	 * The key of the given entry is the index in this tracker where the entry begins.
	 * This may be equal to the given <tt>index</tt>, or less (if {@link TrackPart#getLength()} > 1).
	 * 
	 * @param index The index in this Tracker
	 */
	public abstract Map.Entry<Integer, TrackPart> getEntryAt(int index);
	
	public static void setSource(Object target, int targetIndex, int length, Object source, int sourceIndex) {
		Tracker sourceTracker = TrackerRepository.getTracker(source);
		if (sourceTracker == null) {
			// if we're not tracking the source, we don't care where its content goes to
			// TODO if targetTracker already has info for that range, erase it!
			return;
		}
		
		Tracker targetTracker = TrackerRepository.getOrCreateTracker(target);
		targetTracker.setSourceFromTracker(targetIndex, length, sourceTracker, sourceIndex);
	}
}
