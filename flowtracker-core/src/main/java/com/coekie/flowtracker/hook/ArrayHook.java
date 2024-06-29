package com.coekie.flowtracker.hook;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerUpdater;

/** Hook methods for operations on arrays */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ArrayHook {
  /** Store a value in a char[]. Used by `ArrayStore`. */
  public static void setChar(char[] array, int arrayIndex, char value, TrackerPoint source,
      Context context) {
    array[arrayIndex] = value;
    TrackerUpdater.setSourceTrackerPoint(context, array, arrayIndex, 1, source);
  }

  /** Store a value in a byte[]. Used by `ArrayStore`. */
  public static void setByte(byte[] array, int arrayIndex, byte value, TrackerPoint source,
      Context context) {
    array[arrayIndex] = value;
    TrackerUpdater.setSourceTrackerPoint(context, array, arrayIndex, 1, source);
  }

  /** Store a value in an int[]. Used by `ArrayStore`. */
  public static void setInt(int[] array, int arrayIndex, int value, TrackerPoint source,
      Context context) {
    array[arrayIndex] = value;
    TrackerUpdater.setSourceTrackerPoint(context, array, arrayIndex, 1, source);
  }

  /** Hook for calling clone() on a char[]. Used by `ArrayCloneCall`. */
  public static char[] clone(char[] array) {
    char[] result = array.clone();
    TrackerUpdater.setSource(context(), result, 0, array.length, array, 0);
    return result;
  }

  /** Hook for calling clone() on a byte[]. Used by `ArrayCloneCall`. */
  public static byte[] clone(byte[] array) {
    byte[] result = array.clone();
    TrackerUpdater.setSource(context(), result, 0, array.length, array, 0);
    return result;
  }

  /** Hook for calling clone() on an int[]. Used by `ArrayCloneCall`. */
  public static int[] clone(int[] array) {
    int[] result = array.clone();
    TrackerUpdater.setSource(context(), result, 0, array.length, array, 0);
    return result;
  }
}
