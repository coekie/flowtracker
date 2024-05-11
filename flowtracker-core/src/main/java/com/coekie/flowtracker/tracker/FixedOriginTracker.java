package com.coekie.flowtracker.tracker;

/**
 * OriginTracker that cannot grow.
 */
public class FixedOriginTracker extends OriginTracker {
  private final int length;

  public FixedOriginTracker(int length) {
    this.length = length;
  }

  @Override
  public int getLength() {
    return length;
  }
}
