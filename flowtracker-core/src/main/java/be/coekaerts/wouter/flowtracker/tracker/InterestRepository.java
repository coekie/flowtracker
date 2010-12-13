package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the trackers we're really interested in
 */
public class InterestRepository {
	private static final Collection<ContentTracker> contentTrackers = Collections.newSetFromMap(new ConcurrentHashMap<ContentTracker, Boolean>());
	
	/**
	 * Called when a ContentTracker has been created.
	 * 
	 * This kind of acts as a listener on TrackerRepository.
	 * @param obj The object the tracker was created for
	 * @param tracker The new tracker
	 */
	static void contentTrackerCreated(Object obj, ContentTracker tracker) {
		contentTrackers.add(tracker);
	}
	
	public static Collection<ContentTracker> getContentTrackers() {
		return Collections.unmodifiableCollection(contentTrackers);
	}
}
