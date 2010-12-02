package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Specifies how deep to look for the source of a value.
 */
public interface TrackerDepth {
	/**
	 * Find an immutable tracker. 
	 */
	public static final TrackerDepth CONTENT_IMMUTABLE = new TrackerDepth() {
		@Override
		public boolean isAcceptableContent(Tracker sourceTracker) {
			return ! sourceTracker.isContentMutable();
		}
	};
	
	/**
	 * Go all the way to the origin.
	 */
	public static final TrackerDepth ORIGIN = new TrackerDepth() {
		@Override
		public boolean isAcceptableContent(Tracker sourceTracker) {
			return sourceTracker.getEntryCount() == 0;
		}
	};
	
	/**
	 * Returns if the specified sourceTracker can be part of this tracker.
	 * If not, we have to go deeper: the content of sourceTracker should be used instead.
	 */
	boolean isAcceptableContent(Tracker sourceTracker);
}
