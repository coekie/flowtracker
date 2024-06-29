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

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;

/** Hooks for methods in the `java.lang.Character` class */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class CharacterHook {
  @Hook(target = "java.lang.Character",
      method = "int toCodePoint(char, char)")
  public static void afterToCodePoint(@Arg("INVOCATION") Invocation invocation) {
    TrackerPoint point0 = Invocation.getArgPoint(invocation, 0);
    TrackerPoint point1 = Invocation.getArgPoint(invocation, 1);
    if (point0 != null) {
      // the returned value is a combination of the two arguments.
      // if the two arguments come from the same source, following right after each other:
      if (point1 != null
          && point1.tracker == point0.tracker
          && point1.index == point0.index + point0.length) {
        // then consider the combination of them as the source
        invocation.returnPoint = TrackerPoint.of(point0.tracker, point0.index,
            point0.length + point1.length);
      } else {
        // else use the first point and ignore the second one (because we don't have a way to
        // represent something coming from a combination of two different sources)
        invocation.returnPoint = point0;
      }
    }
  }
}
