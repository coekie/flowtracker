package be.coekaerts.wouter.flowtracker.tracker;

// TODO doc
public final class Growth {
  public static final Growth NONE = new Growth(1, 1);
  public static final Growth DOUBLE = new Growth(2, 1);
  public static final Growth HALF = new Growth(1, 2);

  final int numerator;
  final int denominator;

  private Growth(int numerator, int denominator) {
    this.numerator = numerator;
    this.denominator = denominator;
  }

  public static Growth of(int numerator, int denominator) {
    if (numerator == 1) {
      if (denominator == 1) {
        return NONE;
      } else if (denominator == 2) {
        return HALF;
      }
    } else if (numerator == 2) {
      if (denominator == 1) {
        return DOUBLE;
      }
    }
    return new Growth(numerator, denominator);
  }

  public Growth combine(Growth other) {
    return of(numerator * other.numerator, denominator * other.denominator);
  }

  public Growth half() {
    return of(numerator, denominator * 2);
  }

  // TODO this should probably be more lenient, to account for fractional indexes?
  public boolean lengthMatches(int sourceLength, int targetLength) {
    return sourceLength * numerator == targetLength * denominator;
  }

  public int targetToSource(int targetLength) {
    // TODO should this round up? (+numerator-1 before divide?)
    //  e.g. for DOUBLE, if target length is 5, then 3 source characters are involved
    return targetLength * denominator / numerator;
  }

  public int sourceToTarget(int sourceLength) {
    return sourceLength * numerator / denominator;
  }

  @Override
  public int hashCode() {
    return numerator * 37 + denominator;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Growth)) return false;
    Growth other = (Growth) o;
    return numerator == other.numerator && denominator == other.denominator;
  }

  @Override
  public String toString() {
    return numerator + "/" + denominator;
  }

  /** Represents this Growth as an operation on the source length, e.g. "*2" or "/2" or "*2/3" */
  public String toOperationString() {
    if (numerator == 1) {
      if (denominator == 1) {
        return "";
      } else {
        return "/" + denominator;
      }
    } else if (denominator == 1) {
      return "*" + numerator;
    }
    return "*" + toString();
  }
}
