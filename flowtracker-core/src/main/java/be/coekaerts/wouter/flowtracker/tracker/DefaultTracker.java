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
    tracker.pushSourceTo(0, tracker.getLength(), result, 0);
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
  public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex, Growth growth) {
    if (sourceTracker == null) {
      doSetSource(index, length, null, -1, Growth.NONE);
    } else if (depth.isAcceptableContent(sourceTracker)) {
      if (sourceTracker.isContentMutable()) {
        // we should make a copy here, because if it changes, it won't be correct anymore.
        throw new UnsupportedOperationException(
            "Adding mutable tracker as source is not supported");
      } else {
        doSetSource(index, length, sourceTracker, sourceIndex, growth);
      }
    } else {
      sourceTracker.pushSourceTo(sourceIndex, length, this, index, growth);
    }
  }

  @Override
  // TODO really take growth into account, for the indexes. all usages of targetLength here are
  //  probably wrong
  public void pushSourceTo(int sourceIndex, int targetLength, WritableTracker targetTracker,
      int targetIndex, Growth growth) {
    // we start at the part that contains sourceIndex
    // or, if there's no such part, at what comes after
    Entry<Integer, PartTracker> startEntry = getEntryAt(sourceIndex);
    int startIndex = startEntry == null ? sourceIndex : startEntry.getKey();

    int pos = 0; // how far we are in pushing, from 0 to length
    for (Entry<Integer, PartTracker> entry : map.tailMap(startIndex).entrySet()) {
      int partIndex = entry.getKey();
      PartTracker part = entry.getValue();

      // if we're at a part that's after the range we want to copy, stop
      // TODO[growth] s/targetLength/sourceLength/, probably
      if (partIndex >= sourceIndex + targetLength) {
        break;
      }

      // gap before this entry
      int gapBefore = partIndex - sourceIndex - pos;
      if (gapBefore > 0) {
        targetTracker.setSource(targetIndex + pos, gapBefore, null, -1, Growth.NONE);
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
      int pushLength = Math.min(targetLength - pushingOffset, part.getLength() - pushingPartOffset);

      // push it!
      part.pushSourceTo(pushingPartOffset, pushLength, targetTracker, pushTargetIndex, growth);

      pos = partIndex + part.getLength() - sourceIndex;
    }

    // gap at the end
    if (pos < targetLength) {
      targetTracker.setSource(targetIndex + pos, targetLength - pos, null, -1);
    }
  }

  private void doSetSource(int index, int length, Tracker sourceTracker, int sourceIndex,
      Growth growth) {
    if (length == 0) return;

    // check the entry right after the new one (starting where new one ends, or overlapping)
    Entry<Integer, PartTracker> nextEntry = getEntryAt(index + length);
    if (nextEntry != null) {
      PartTracker nextPart = nextEntry.getValue();
      if (nextPart.getTracker() == sourceTracker // same source
          && nextPart.getGrowth().equals(growth)
          // and indexes align: difference in source is same as in target (scaled by growth)
          && growth.lengthMatches(sourceIndex - nextPart.getSourceIndex(),
              index - nextEntry.getKey())) {
        // merge two parts together by extending length
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
        // TODO[growth] verify behaviour
        // TODO[growth] do something special if we're cutting nextPart at not-a-block-boundary
        map.put(newStart, new PartTracker(nextPart.getTracker(),
            nextPart.getSourceIndex() + growth.targetToSource(delta),
            nextPart.getLength() - delta,
            growth));
      }
    }

    // remove parts that start within the new part, they are overwritten
    map.subMap(index, index + length).clear();

    // check the entry before the new one
    Entry<Integer, PartTracker> previousEntry = getEntryAt(index - 1);
    if (previousEntry != null) {
      PartTracker previousPart = previousEntry.getValue();
      if (previousPart.getTracker() == sourceTracker // same source
          && previousPart.getGrowth().equals(growth)
          // and indexes align: difference in source index is same as difference in target (scaled by growth)
          && growth.lengthMatches(sourceIndex - previousPart.getSourceIndex(),
              index - previousEntry.getKey())) {
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
      map.put(index, new PartTracker(sourceTracker, sourceIndex, length, growth));
    }
  }

  /**
   * Returns the entry in the tracker for the given index.
   *
   * The key of the given entry is the index in this tracker where the entry begins.
   * This may be equal to the given <tt>index</tt>, or less (if {@link PartTracker#getLength()} >
   * 1).
   *
   * @param index The index in this Tracker
   */
  private Entry<Integer, PartTracker> getEntryAt(int index) {
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
