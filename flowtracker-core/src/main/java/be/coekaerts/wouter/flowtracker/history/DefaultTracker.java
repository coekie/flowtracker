package be.coekaerts.wouter.flowtracker.history;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public class DefaultTracker extends Tracker {
	private NavigableMap<Integer, TrackPart> map = new TreeMap<Integer, TrackPart>();
	
	public void setSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex) {
		// TODO check if can be merged with entry before it
		// TODO check if can be merged with entry after it
		boolean stored = false;
		
		// extend the entry right before it, if it matches
		Entry<Integer, TrackPart> entryBefore = getEntryAt(index - 1);
		if (entryBefore != null) {
			TrackPart partBefore = entryBefore.getValue();
			if (partBefore.getTracker() == sourceTracker &&
					(index - entryBefore.getKey() == sourceIndex - partBefore.getIndex())) {
				partBefore.setLength(partBefore.getLength() + length); // TODO this is wrong if they are overlapping
				stored = true;
			}
		}
		
		// extend the entry right after it (backwards), if it matches
		Entry<Integer, TrackPart> entryAfter = getEntryAt(index + length);
		if (entryAfter != null) {
			TrackPart partAfter = entryAfter.getValue();
			if (partAfter.getTracker() == sourceTracker &&
					(index - entryAfter.getKey() == sourceIndex - partAfter.getIndex())) {
				map.remove(entryAfter.getKey());
				partAfter.setLength(partAfter.getLength() + length); // TODO this is wrong if they are overlapping
				map.put(index, partAfter);
				stored = true;
			}
		}
		
		// TODO remove/adjust overlapping parts
		
		if (!stored) {
			TrackPart entry = new TrackPart(sourceTracker, sourceIndex, length);
			map.put(index, entry);
		}
	}
	
	@Override
	public Entry<Integer, TrackPart> getEntryAt(int index) {
		Entry<Integer, TrackPart> floorEntry = map.floorEntry(index);
		if (floorEntry == null) {
			// there is no entry at or before index
			return null;
		} else {
			int partPosition = floorEntry.getKey();
			if (partPosition + floorEntry.getValue().getLength() <= index) {
				// the entry before this index is not long enough to cover this one
				return null;
			} else {
				// we have match
				return floorEntry;
			}
		}
	}
}
