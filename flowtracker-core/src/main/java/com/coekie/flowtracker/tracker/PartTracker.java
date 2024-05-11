package com.coekie.flowtracker.tracker;

/** Part of a {@link Tracker} that comes from one source. */
public class PartTracker extends Tracker {
  private final Tracker tracker;
  private final int sourceIndex;
  private int length;
  private final Growth growth;

  public PartTracker(Tracker tracker, int sourceIndex, int length, Growth growth) {
    this.tracker = tracker;
    this.sourceIndex = sourceIndex;
    this.length = length;
    this.growth = growth;
  }

  /**
   * The tracker that this part points to.
   * <p>
   * This is the source; this is <b>not</b> the tracker of which this is a part.
   */
  public Tracker getTracker() {
    return tracker;
  }

  /**
   * The index in {@link #getTracker()} where this part starts
   */
  public int getSourceIndex() {
    return sourceIndex;
  }

  /** The length of this part */
  public int getLength() {
    return length;
  }

  void setLength(int length) {
    this.length = length;
  }

  Growth getGrowth() {
    return growth;
  }

  @Override
  public int getEntryCount() {
    return 1;
  }

  @Override
  public void pushSourceTo(int partIndex, int targetLength, WritableTracker targetTracker,
      int targetIndex, Growth growth) {
    Growth combinedGrowth = this.growth.combine(growth);

    // clarification:
    // we're dealing with three trackers here: the source, this tracker (us), and the target.
    // this.growth is about the relation between source and us.
    // growth (parameter) is about the relation between us and target.

    // handle case where the range we're pushing does not start at a "block" boundary in this part.
    // for example, if this.growth is Growth.DOUBLE, and partIndex is uneven, then we first need to
    // push _half_ a block (at partIndex, length one) with an adjusted Growth that indicates where
    // that first part came from (Growth.NONE in this example), and then the rest that is aligned
    // with block boundaries, with the normal growth.
    int startMisalignment = partIndex % this.growth.targetBlock;
    if (startMisalignment != 0) {
      int startMisalignedLength = this.growth.targetBlock - startMisalignment;
      // TODO deal with the case where startMisalignedLength is not multiple of
      //   growth.sourceBlock. in other words, if the misalignment of `this.growth` is misaligned
      //   with `growth`. ignoring that for now. (and same for misalignment at the middle or end)
      int startMisalignedTargetLength =
          Math.min(growth.sourceToTarget(startMisalignedLength), targetLength);

      targetTracker.setSource(targetIndex, startMisalignedTargetLength, tracker,
          this.sourceIndex + this.growth.targetToSource(partIndex - startMisalignment),
          Growth.of(startMisalignedTargetLength, this.growth.sourceBlock));
      // adjust parameters for the remaining part that needs to be pushed
      partIndex += startMisalignedLength;
      targetIndex += startMisalignedTargetLength;
      targetLength -= startMisalignedTargetLength;

      if (targetLength <= 0) { // if there's nothing left
        return;
      }
    }

    // similar for if we're misaligned at the end
    int ourLength = growth.targetToSource(targetLength);
    int endMisalignedLength = ourLength % this.growth.targetBlock;
    if (endMisalignedLength != 0) {
      int endMisalignedTargetLength = growth.sourceToTarget(endMisalignedLength);

      // the main (middle) piece
      int adjustedTargetLength = targetLength - endMisalignedTargetLength;
      targetTracker.setSource(targetIndex, adjustedTargetLength, tracker,
          this.sourceIndex + this.growth.targetToSource(partIndex),
          combinedGrowth);

      // the piece that's misaligned at the end
      targetTracker.setSource(targetIndex + adjustedTargetLength,
          endMisalignedTargetLength, tracker,
          this.sourceIndex + this.growth.targetToSource(partIndex)
              + this.growth.targetToSource(ourLength - endMisalignedLength),
          Growth.of(endMisalignedTargetLength, this.growth.sourceBlock));
    } else {
      // standard case, no misalignment (hit >99% of the time)
      targetTracker.setSource(targetIndex, targetLength, tracker,
          this.sourceIndex + this.growth.targetToSource(partIndex),
          combinedGrowth);
    }
  }
}
