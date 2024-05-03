package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class CharacterHook {
  @Hook(target = "java.lang.Character",
      method = "int toCodePoint(char, char)")
  public static void afterToCodePoint(@Arg("INVOCATION") Invocation invocation) {
    TrackerPoint point = Invocation.getArgPoint(invocation, 0);
    if (point != null) {
      // the returned value is a combination of the two arguments. for simplicity, we assume/pretend
      // that the second argument comes from the same source, with the same length.
      // TODO(growth) if this is combining two characters that in their source were encoded using a
      //  different number of bytes, then this is actually incorrect.
      invocation.returnPoint = TrackerPoint.of(point.tracker, point.index, point.length * 2);
    }
  }
}
