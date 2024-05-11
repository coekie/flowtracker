package com.coekie.flowtracker.tracker;

/**
 * Specifies, for a range in the target tracker pointing to a range in the source tracker,
 * how indexes in the target correspond to indexes in the source. Each consecutive set of
 * `targetBlock` indexes (e.g. chars, bytes,...) in the target come from `sourceBlock` indexes in
 * the source.
 */
public final class Growth {
  /**
   * Indicates there is a one-to-one relation between indexes in source and target. Index `n` of the
   * range in the source corresponds to index `n` of the range in the target.
   */
  public static final Growth NONE = new Growth(1, 1);

  /**
   * Indicates there is a two-to-one relation between indexes in target and source. Index `n` of the
   * range in the source corresponds to index `n*2` and `n*2+1` of the range in the target.
   */
  public static final Growth DOUBLE = new Growth(2, 1);

  /**
   * Indicates there is a one-to-two relation between indexes in target and source. Index `n` and
   * `n+1` of the range in the source corresponds to index `floor(n/2)` in the target.
   */
  public static final Growth HALF = new Growth(1, 2);

  /** Size of one block (of bytes, chars,...) in the target tracker */
  final int targetBlock;
  /** Size of one block (of bytes, chars,...) in the source tracker */
  final int sourceBlock;

  private Growth(int targetBlock, int sourceBlock) {
    this.targetBlock = targetBlock;
    this.sourceBlock = sourceBlock;
  }

  public static Growth of(int targetBlock, int sourceBlock) {
    // reuse existing instances in the common cases
    if (targetBlock == 1) {
      if (sourceBlock == 1) {
        return NONE;
      } else if (sourceBlock == 2) {
        return HALF;
      }
    } else if (targetBlock == 2) {
      if (sourceBlock == 1) {
        return DOUBLE;
      }
    }
    return new Growth(targetBlock, sourceBlock);
  }

  public Growth combine(Growth other) {
    if (this == NONE && other == NONE) { // fast path
      return NONE;
    }
    return of(targetBlock * other.targetBlock, sourceBlock * other.sourceBlock);
  }

  /**
   * Returns if a length in the source tracker corresponds (exactly) to a length in the target
   * tracker
   */
  public boolean lengthMatches(int sourceLength, int targetLength) {
    return sourceLength * targetBlock == targetLength * sourceBlock;
  }

  /**
   * Maps a length (or index) in the target range to one in the source range.
   * If it corresponds to multiple indexes (e.g. {@link Growth#HALF}) then this is the first one.
   * This only works correctly/exactly if targetLength is a multiple of {@link #targetBlock}.
   */
  public int targetToSource(int targetLength) {
    return targetLength * sourceBlock / targetBlock;
  }

  /**
   * Maps a length (or index) in the source range to one in the target range.
   * If it corresponds to multiple indexes (e.g. {@link Growth#DOUBLE}) then this is the first one.
   * This only works correctly/exactly if sourceLength is a multiple of {@link #sourceBlock}.
   */
  public int sourceToTarget(int sourceLength) {
    return sourceLength * targetBlock / sourceBlock;
  }

  @Override
  public int hashCode() {
    return targetBlock * 37 + sourceBlock;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Growth)) return false;
    Growth other = (Growth) o;
    return targetBlock == other.targetBlock && sourceBlock == other.sourceBlock;
  }

  @Override
  public String toString() {
    return targetBlock + "/" + sourceBlock;
  }

  /** Represents this Growth as an operation on the source length, e.g. "*2" or "/2" or "*2/3" */
  public String toOperationString() {
    if (targetBlock == 1) {
      if (sourceBlock == 1) {
        return "";
      } else {
        return "/" + sourceBlock;
      }
    } else if (sourceBlock == 1) {
      return "*" + targetBlock;
    }
    return "*" + this;
  }
}
