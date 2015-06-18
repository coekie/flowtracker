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
  public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex) {
    if (sourceTracker == null) {
      doSetSource(index, length, null, -1);
    } else if (depth.isAcceptableContent(sourceTracker)) {
      if (sourceTracker.isContentMutable()) {
        // we should make a copy here, because if it changes, it won't be correct anymore.
        throw new UnsupportedOperationException(
            "Adding mutable tracker as source is not supported");
      } else {
        doSetSource(index, length, sourceTracker, sourceIndex);
      }
    } else {
      sourceTracker.pushContentToTracker(sourceIndex, length, this, index);
    }
  }

  @Override
  public void pushContentToTracker(int sourceIndex, int length, WritableTracker targetTracker,
      int targetIndex) {
    // we start at the part that contains sourceIndex
    // or, if there's no such part, at what comes after
    Entry<Integer, PartTracker> startEntry = getEntryAt(sourceIndex);
    int startIndex = startEntry == null ? sourceIndex : startEntry.getKey();

    int pos = sourceIndex; // TODO simplify by keeping track of pos in target (-sourceIndex + targetIndex) ?
    for (Entry<Integer, PartTracker> entry : map.tailMap(startIndex).entrySet()) {
      int partIndex = entry.getKey();
      PartTracker part = entry.getValue();

      // if we're at a part that's after the range we want to copy, stop
      if (partIndex >= sourceIndex + length) {
        break;
      }

      // gap before this entry
      if (partIndex > pos) {
        targetTracker.setSource(targetIndex + pos - sourceIndex,
            Math.min(partIndex, sourceIndex + length) - pos, null, -1);
      }

      // if the beginning of this part is cut off (because this it the first part, and sourceIndex
      // is halfway a part), the size of the cut-off.
      // In other words, the offset in the part of where we want to start pushing.
      int pushingPartOffset = partIndex < sourceIndex ? sourceIndex - partIndex : 0;

      // difference between what we'll start pushing for this part, and where we started overall.
      // in other words our progress, going from 0 to length.
      int pushingOffset = (partIndex + pushingPartOffset) - sourceIndex;

      // index in the target of where we start pushing to
      int pushTargetIndex = targetIndex + pushingOffset;

      // The length of what we're pushing. This is limited by two things:
      // * the length given in arguments
      // * the available size of the part we're handling
      int pushLength = Math.min(length - pushingOffset, part.getLength() - pushingPartOffset);

      // push it!
      part.pushContentToTracker(pushingPartOffset, pushLength, targetTracker, pushTargetIndex);

      pos = partIndex + part.getLength();
    }

    // gap at the end
    if (pos < sourceIndex + length) {
      targetTracker.setSource(targetIndex + pos - sourceIndex, sourceIndex + length - pos,
          null, -1);
    }
  }

  private void doSetSource(int index, int length, Tracker sourceTracker, int sourceIndex) {
    if (length == 0) return;

    // check the entry right after the new one
    Entry<Integer, PartTracker> nextEntry = getEntryAt(index + length);
    if (nextEntry != null) {
      PartTracker nextPart = nextEntry.getValue();
      if (nextPart.getTracker() == sourceTracker && // same source
          // and relative index is the same (no gaps or skipping in the source)
          (index - nextEntry.getKey() == sourceIndex - nextPart.getIndex())) {
        // if it aligns with the new one, merge two parts together by extending length
        // note: old nextEntry itself will be removed below
        length = nextEntry.getKey() + nextPart.getLength() - index;
      } else if (nextEntry.getKey() != index + length) {
        // else if it overlaps (so its start or middle will be overwritten),
        // then add the remaining part of nextEntry (cut start off its beginning),
        // starting where the new one ends
        // note: old nextEntry itself will be removed below
        int oldStart = nextEntry.getKey();
        int newStart = index + length;
        int delta = newStart - oldStart;
        map.put(newStart, new PartTracker(nextPart.getTracker(),
            nextPart.getIndex() + delta,
            nextPart.getLength() - delta));
      }
    }

    // remove parts that start within the new part, they are overwritten
    map.subMap(index, index + length).clear();

    // check the entry before the new one
    Entry<Integer, PartTracker> previousEntry = getEntryAt(index - 1);
    if (previousEntry != null) {
      PartTracker previousPart = previousEntry.getValue();
      if (previousPart.getTracker() == sourceTracker && // same source
          // and relative index is the same (no gaps or skipping in the source)
          (index - previousEntry.getKey() == sourceIndex - previousPart.getIndex())) {
        // then extend the entry before it by extending its length
        previousPart.setLength(index + length - previousEntry.getKey());
        return; // no need to add the entry anymore
      } else if (previousEntry.getKey() + previousPart.getLength() > index) {
        // cut end off of previous part if this overwrites it
        previousPart.setLength(index - previousEntry.getKey());
      }
    }

    // add the new entry
    if (sourceTracker != null) {
      map.put(index, new PartTracker(sourceTracker, sourceIndex, length));
    }
  }

  @Override
  public Entry<Integer, PartTracker> getEntryAt(int index) {
    Entry<Integer, PartTracker> floorEntry = map.floorEntry(index);
    // if the entry that starts at or before (floor) also extends (key + length) up to index
    if (floorEntry != null && floorEntry.getKey() + floorEntry.getValue().getLength() > index) {
      return floorEntry;
    } else {
      return null;
    }
  }

  @Override
  public int getEntryCount() {
    return map.size();
  }

  @Override
  public int getLength() {
    if (map.isEmpty()) return 0;
    Entry<Integer, PartTracker> lastEntry = map.lastEntry();
    return lastEntry.getKey() + lastEntry.getValue().getLength();
  }
}
