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

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import java.lang.invoke.MethodHandles;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ConstantHook {
  // first three arguments are here because this is invoked using ConstantDynamic
  public static TrackerPoint constantPoint(MethodHandles.Lookup lookup, String name,
      Class<?> type, int classId, int offset, int length) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset, length);
  }

  // variant for class file versions that don't support ConstantDynamic
  public static TrackerPoint constantPoint(int classId, int offset) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset, 1);
  }

  // we'd rather hava constantPoint(classId, offset, length), but because of limitations of how much
  // stack we can use in ConstantValue.loadSourcePoint we split that into two calls when length
  // is not 1
  public static TrackerPoint withLength(TrackerPoint point, int length) {
    return TrackerPoint.of(point.tracker, point.index, length);
  }
}
