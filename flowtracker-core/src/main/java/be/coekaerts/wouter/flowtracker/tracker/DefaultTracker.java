package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class DefaultTracker extends Tracker {
	private final NavigableMap<Integer, PartTracker> map = new TreeMap<Integer, PartTracker>();
	private final TrackerDepth depth;
	
	/**
	 * Create a tracker whose content is a copy of the given tracker, with the specified depth.  
	 */
	public static DefaultTracker copyOf(Tracker tracker, TrackerDepth depth) {
		DefaultTracker result = new DefaultTracker(depth);
		tracker.pushContentToTracker(0, tracker.getLength(), result, 0);
		return result;
	}
	
	public DefaultTracker() {
		this(TrackerDepth.CONTENT_IMMUTABLE);
	}
	
	public DefaultTracker(TrackerDepth depth) {
		super();
		this.depth = depth;
	}
	
	@Override
	public void setSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex) {
		if (depth.isAcceptableContent(sourceTracker)) {
			if (sourceTracker.isContentMutable()) {
				// we should make a copy here, because if it changes, it won't be correct anymore.
				throw new UnsupportedOperationException("Adding mutable tracker as source is not supported");
			} else {
				doSetSourceFromTracker(index, length, sourceTracker, sourceIndex);
			}
		} else {
			sourceTracker.pushContentToTracker(sourceIndex, length, this, index);
		}
	}
	
	@Override
	public void pushContentToTracker(int sourceIndex, int length, Tracker targetTracker, int targetIndex) {
		// we start at the part that contains sourceIndex
		int startIndex = getStartIndexAt(sourceIndex);
		// or, if there's no such part, at what comes after
		if (startIndex == -1)
			startIndex = sourceIndex;
		
		for (Entry<Integer, PartTracker> entry : map.tailMap(startIndex).entrySet()) {
			int partIndex = entry.getKey();
			PartTracker part = entry.getValue();
			
			// if we're at a part that's after the range we want to copy, stop
			if (partIndex >= sourceIndex + length) {
				break;
			}
			
			// if the beginning of this part is cut off (because this it the first part, and sourceIndex is halfway a part),
			// the size of the cut-off.
			// In other words, the offset in the part of where we want to start pushing.
			int pushingPartOffset = partIndex < sourceIndex ? sourceIndex - partIndex : 0;
			
			// the difference between what we'll start pushing and the start of the range
			int pushingOffset = (partIndex + pushingPartOffset) - sourceIndex;
			
			// index in the target of where we start pushing to
			int pushTargetIndex = targetIndex + pushingOffset;
			
			// The length of what we're pushing. This is limited by two things:
			// * the length given in arguments
			// * the available size of the part we're handling
			int pushLength = Math.min(length - pushingOffset, part.getLength() - pushingPartOffset);
			
			// push it!
			targetTracker.setSourceFromTracker(pushTargetIndex, pushLength,	part, pushingPartOffset);
		}
	}
	
	private void doSetSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex) {
		boolean stored = false;
		
		// extend the entry right before it, if it matches
		Entry<Integer, PartTracker> entryBefore = getEntryAt(index - 1);
		if (entryBefore != null) {
			PartTracker partBefore = entryBefore.getValue();
			if (partBefore.getTracker() == sourceTracker &&
					(index - entryBefore.getKey() == sourceIndex - partBefore.getIndex())) {
				partBefore.setLength(partBefore.getLength() + length); // TODO this is wrong if they are overlapping
				stored = true;
			}
		}
		
		// extend the entry right after it (backwards), if it matches
		Entry<Integer, PartTracker> entryAfter = getEntryAt(index + length);
		if (entryAfter != null) {
			PartTracker partAfter = entryAfter.getValue();
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
			PartTracker entry = new PartTracker(sourceTracker, sourceIndex, length);
			map.put(index, entry);
		}
	}
	
	@Override
	public Entry<Integer, PartTracker> getEntryAt(int index) {
		Entry<Integer, PartTracker> floorEntry = map.floorEntry(index);
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
	
	@Override
	public int getStartIndexAt(int index) {
		Entry<Integer, PartTracker> entry = getEntryAt(index);
		return entry == null ? -1 : entry.getKey();
	}
	
	@Override
	public int getEntryCount() {
		return map.size();
	}
	
	@Override
	public int getLength() {
		// TODO getLength
		throw new RuntimeException("not implemented");
	}
}
