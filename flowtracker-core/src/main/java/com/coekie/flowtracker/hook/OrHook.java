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

/** For OrValue, handling a binary or (|) operation */
@SuppressWarnings("unused") // used in OrValue
public class OrHook {
  public static TrackerPoint or(TrackerPoint point1, TrackerPoint point2) {
    if (point1 == null) {
      return point2;
    } else if (point2 == null) {
      return point1;
    } else if (point1.tracker != point2.tracker) {
      // if two points don't come from the same tracker, then we cannot represent them as one point
      return null;
    } else if (point1.index + point1.length == point2.index) {
      // if the points are for values right next to each other,
      // then merge them into a pointer with combined length
      return point1.withLength(point1.length + point2.length);
    } else if (point2.index + point2.length == point1.index) { // other order
      return point2.withLength(point1.length + point2.length);
    } else {
      return null;
    }
  }
}
