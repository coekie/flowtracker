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

import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatException;
import java.lang.invoke.StringConcatFactory;

/**
 * Hook for {@link StringConcatFactory}, which is used for implementing String
 * concatenation.
 */
public class StringConcatFactoryHook {
  /** Handle to {@link #trackedCharToString(char, TrackerPoint)} */
  private static final MethodHandle trackedCharToString;
  static {
    try {
      trackedCharToString = MethodHandles.lookup().findStatic(StringConcatFactoryHook.class,
          "trackedCharToString",
          MethodType.methodType(String.class, char.class, TrackerPoint.class));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /**
   * Hook for {@link
   * StringConcatFactory#makeConcatWithConstants(Lookup, String, MethodType, String, Object...)}.
   * See docs on that method for meaning of the recipe and constants etc.
   * Differences between this hooked version and the real one:<ul>
   * <li>We add an extra `mask` parameter. See {@link #parseMask(String)}.
   * <li>When there are tracked char values then the returned MethodHandle expects the corresponding
   * TrackerPoints as extra arguments at the end.
   * </ul>
   * <p>
   * This is implemented by converting every tracked `char` to a String (to which, unlike a
   * primitive `char` we can attach a tracker).
   */
  public static CallSite makeConcatWithConstants(MethodHandles.Lookup lookup,
      String name,
      MethodType concatType,
      String recipe,
      String mask,
      Object... constants) throws StringConcatException {
    int[] trackedParams = parseMask(mask);

    // since MethodHandles are constructed backwards, this code is easier to understand reading
    // backwards, starting at "Step 1".

    // Step 3: Call the real makeConcatWithConstants (with the Strings instead of chars).
    MethodType newConcatType = newConcatType(concatType, trackedParams);
    MethodHandle mh = StringConcatFactory.makeConcatWithConstants(lookup, name, newConcatType,
        recipe, constants).getTarget();

    // Step 2: Combine the char+TrackerPoint pairs into Strings. We're looping backwards here
    // because MethodHandles are constructed backwards. When executing, the first pair will be
    // combined first. (therefor all tracked chars before a one we are combining has already been
    // combined, therefor we don't need to adjust parameter positions: the char+TrackerPoint are at
    // positions trackedParams[i] and trackedParams[i]+1).
    for (int i = trackedParams.length - 1; i >= 0; i--) {
      mh = MethodHandles.collectArguments(mh, trackedParams[i], trackedCharToString);
    }

    // Step 1: Permute the order of arguments, so that for each tracked `char`, it is immediately
    // followed by its corresponding TrackerPoint.
    // for example if we take as incoming arguments (int,char,String,TrackerPoint), and the char is
    // tracked, then that's permuted to (int,char,TrackerPoint,String).
    int[] permutation = new int[concatType.parameterCount()];
    int newLocation = 0;
    int nextPointTrackerParam = mask.length();
    for (int param = 0; param < mask.length(); param++) {
      permutation[newLocation++] = param;
      if (mask.charAt(param) == 'T') {
        permutation[newLocation++] = nextPointTrackerParam++;
      }
    }
    mh = MethodHandles.permuteArguments(mh, concatType, permutation);

    return new ConstantCallSite(mh);
  }

  /** Determine the `concatType` for the real makeConcatWithConstants. */
  private static MethodType newConcatType(MethodType concatType, int[] trackedParams) {
    // Remove the trailing TrackerPoint parameters that our hook gets called with
    for (int i = concatType.parameterCount() - trackedParams.length;
        i < concatType.parameterCount(); i++) {
      if (concatType.parameterType(i) != TrackerPoint.class) {
        throw new Error("Expected " + trackedParams.length + " TrackerPoint parameters on "
            + concatType);
      }
    }
    MethodType newConcatType = concatType.dropParameterTypes(
        concatType.parameterCount() - trackedParams.length,
        concatType.parameterCount());

    // replace the tracked char parameters with Strings
    for (int param : trackedParams) {
      if (newConcatType.parameterType(param) != char.class) {
        throw new Error("Unexpected parameter type " + newConcatType.parameterType(param) + " at "
            + param + " in " + newConcatType);
      }
      newConcatType = newConcatType.changeParameterType(param, String.class);
    }
    return newConcatType;
  }

  /**
   * Converts the mask to a list of indices of tracked arguments.
   * The mask is a String in which each character represents if the argument at that index is
   * tracked or not. 'T' indicates a tracked argument, '.' a non-tracked one.
   * e use a String for that because that's something that's easily passed into a bootstrap method.
   * <p>
   * For example: if argument 0 and 2 of a concatenation with four arguments are tracked, then the
   * mask would be "T.T.", and this method would convert that to {0, 2}.
   */
  private static int[] parseMask(String mask) {
    int count = 0;
    char[] chars = mask.toCharArray();
    for (char c : chars) {
      if (c == 'T') {
        count++;
      }
    }
    int[] result = new int[count];
    int r = 0;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == 'T') {
        result[r++] = i;
      }
    }
    return result;
  }

  /**
   * Converts a `char` to a String containing that char, with source set to the given point.
   */
  public static String trackedCharToString(char c, TrackerPoint point) {
    char[] array = new char[]{c};
    TrackerUpdater.setSourceTrackerPoint(array, 0, 1, point);
    return new String(array);
  }
}
