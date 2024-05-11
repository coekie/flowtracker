package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerUpdater;

/** Hook methods for operations on arrays */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ArrayHook {
  /** Store a value in a char[] */
  public static void setChar(char[] array, int arrayIndex, char value, TrackerPoint source) {
    array[arrayIndex] = value;
    TrackerUpdater.setSourceTrackerPoint(array, arrayIndex, 1, source);
  }

  /** Store a value in a byte[] */
  public static void setByte(byte[] array, int arrayIndex, byte value, TrackerPoint source) {
    array[arrayIndex] = value;
    TrackerUpdater.setSourceTrackerPoint(array, arrayIndex, 1, source);
  }

  /** Store a value in an int[] */
  public static void setInt(int[] array, int arrayIndex, int value, TrackerPoint source) {
    array[arrayIndex] = value;
    TrackerUpdater.setSourceTrackerPoint(array, arrayIndex, 1, source);
  }

  /** Hook for calling clone() on a char[] */
  public static char[] clone(char[] array) {
    char[] result = array.clone();
    TrackerUpdater.setSource(result, 0, array.length, array, 0);
    return result;
  }

  /** Hook for calling clone() on a byte[] */
  public static byte[] clone(byte[] array) {
    byte[] result = array.clone();
    TrackerUpdater.setSource(result, 0, array.length, array, 0);
    return result;
  }

  /** Hook for calling clone() on an int[] */
  public static int[] clone(int[] array) {
    int[] result = array.clone();
    TrackerUpdater.setSource(result, 0, array.length, array, 0);
    return result;
  }
}
