package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;

/** Hook methods for operations on arrays */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ArrayHook {
  /** Store a value in a char[] */
  public static void setChar(char[] array, int arrayIndex, char value, Tracker source,
      int sourceIndex) {
    array[arrayIndex] = value;

    TrackerUpdater.setSourceTracker(array, arrayIndex, 1, source, sourceIndex);
  }

  /** Store a value in a byte[] */
  public static void setByte(byte[] array, int arrayIndex, byte value, Tracker source,
      int sourceIndex) {
    array[arrayIndex] = value;

    TrackerUpdater.setSourceTracker(array, arrayIndex, 1, source, sourceIndex);
  }
}
