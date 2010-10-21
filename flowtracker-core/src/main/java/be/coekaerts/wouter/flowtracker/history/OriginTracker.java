package be.coekaerts.wouter.flowtracker.history;

import java.util.Map.Entry;

/**
 * Unmodifiable tracker without a source. 
 */
public class OriginTracker extends Tracker {

	@Override
	void setSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<Integer, TrackPart> getEntryAt(int index) {
		return null;
	}
	
	@Override
	public int getEntryCount() {
		return 0;
	}
}
