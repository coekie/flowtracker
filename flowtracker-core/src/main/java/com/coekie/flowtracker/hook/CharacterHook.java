package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;

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
