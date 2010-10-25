package be.coekaerts.wouter.flowtracker.tracker;

import java.util.IdentityHashMap;
import java.util.Map;

public class TrackerRepository {
	private static final Map<Object, Tracker> objectToTracker = new IdentityHashMap<Object, Tracker>();
	
	public static Tracker getTracker(Object obj) {
		return objectToTracker.get(obj);
	}
	
	public static Tracker createTracker(Object obj) {
		Tracker tracker = new DefaultTracker();
		setTracker(obj, tracker);
		return tracker;
	}
	
	public static Tracker getOrCreateTracker(Object obj) {
		Tracker tracker = getTracker(obj);
		if (tracker == null) {
			return createTracker(obj);
		} else {
			return tracker;
		}
	}
	
	public static ContentTracker getOrCreateContentTracker(Object obj) {
		ContentTracker tracker = (ContentTracker) getTracker(obj);
		if (tracker == null) {
			tracker = new ContentTracker();
			setTracker(obj, tracker);
		}
		return tracker;
	}
	
	public static void setTracker(Object obj, Tracker tracker) {
		if (obj == null) {
			throw new NullPointerException("Can't track null");
		} else if (getTracker(obj) != null) {
			throw new IllegalStateException("Object already has a tracker: " + obj);
		} else {
			objectToTracker.put(obj, tracker);
		}
	}
}
