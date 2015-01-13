package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class DefaultTracker extends Tracker {
	private final NavigableMap<Integer, PartTracker> map = new ConcurrentSkipListMap<>();
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
    // or, if there's no such part, at what comes after
    Entry<Integer, PartTracker> startEntry = getEntryAt(sourceIndex);
    int startIndex = startEntry == null ? sourceIndex : startEntry.getKey();

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
		Entry<Integer, PartTracker> previousEntry = getEntryAt(index - 1);
		if (previousEntry != null) {
			PartTracker previousPart = previousEntry.getValue();
			if (previousPart.getTracker() == sourceTracker && // same source
          // and relative index is the same (no gaps or skipping in the source)
					(index - previousEntry.getKey() == sourceIndex - previousPart.getIndex())) {
        // update length, index and sourceIndex variables so the check below for merging with the
        // next entry works too
        length = index + length - previousEntry.getKey();
        index = previousEntry.getKey();
        sourceIndex = previousPart.getIndex();
        previousPart.setLength(length);
        stored = true;
			} else if (previousEntry.getKey() + previousPart.getLength() > index) {
        // cut end off of previous part if this overwrites it
        previousPart.setLength(index - previousEntry.getKey());
      }
		}
		
		// extend the entry right after it (backwards), if it matches
		Entry<Integer, PartTracker> nextEntry = getEntryAt(index + length);
		if (nextEntry != null) {
			PartTracker nextPart = nextEntry.getValue();
			if (nextPart.getTracker() == sourceTracker &&
					(index - nextEntry.getKey() == sourceIndex - nextPart.getIndex())) {
        nextPart.setLength(nextEntry.getKey() + nextPart.getLength() - index);
        nextPart.setIndex(sourceIndex);
        map.remove(nextEntry.getKey());
				map.put(index, nextPart);
				stored = true;
			}
		}
		
		// other overlapping parts: remove or reduce them
    for (Entry<Integer, PartTracker> e = map.higherEntry(index);
        e != null && e.getKey() < index + length;
        e = map.higherEntry(e.getKey())) {
      PartTracker overlappedPart = e.getValue();
      if (e.getKey() + overlappedPart.getLength() < index + length) {
        map.remove(e.getKey());
      } else {
        // cut start off of next part if this overwrites it
        overlappedPart.setLength(e.getKey() + overlappedPart.getLength() - index - length);
        overlappedPart.setIndex(overlappedPart.getIndex() + index + length - e.getKey());
        map.remove(e.getKey());
        map.put(index + length, overlappedPart);
      }
    }

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

  public NavigableMap<Integer, PartTracker> getMap() {
    return map;
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
