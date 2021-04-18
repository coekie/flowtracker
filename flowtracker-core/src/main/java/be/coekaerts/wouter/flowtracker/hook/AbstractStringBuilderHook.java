package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import java.lang.reflect.Field;

/** Hook methods called by instrumented code for {@link StringBuilder} and {@link StringBuffer} */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class AbstractStringBuilderHook {
  private static final Field valueField;

  static {
    try {
      valueField = StringBuilder.class.getSuperclass().getDeclaredField("value");
      valueField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new Error("Cannot find AbstractStringBuilder.value", e);
    }
  }

  public static StringBuilder append(StringBuilder sb, char c, Tracker source, int sourceIndex) {
    sb.append(c);
    trackAppendChar(sb, sb.length(), c, source, sourceIndex);
    return sb;
  }

  public static StringBuffer append(StringBuffer sb, char c, Tracker source, int sourceIndex) {
    sb.append(c);
    trackAppendChar(sb, sb.length(), c, source, sourceIndex);
    return sb;
  }

  private static void trackAppendChar(Object sb, int sbLength, char c, Tracker source, int sourceIndex) {
    try {
      TrackerUpdater.setSourceTracker(valueField.get(sb), sbLength - 1, 1, source, sourceIndex);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    }
  }
}
