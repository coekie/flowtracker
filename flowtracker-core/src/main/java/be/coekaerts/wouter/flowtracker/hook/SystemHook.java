package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SystemHook {
  @SuppressWarnings("SuspiciousSystemArraycopy")
  public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
    System.arraycopy(src, srcPos, dest, destPos, length);
    TrackerUpdater.setSource(dest, destPos, length, src, srcPos);
  }

  public static void initialize() {
    // we have to add trackers to the existing streams, because they were created before flowtracker
    // was initialized.
    addSystemOutErrTracker(System.out, "System.out");
    addSystemOutErrTracker(System.err, "System.err");
  }

  private static void addSystemOutErrTracker(PrintStream printStream, String name) {
    try {
      Field charOutField = PrintStream.class.getDeclaredField("charOut");
      OutputStreamWriterHook.createOutputStreamWriterTracker(
          (OutputStreamWriter) Reflection.getField(printStream, charOutField))
          .initDescriptor(name + ".charOut", null);
    } catch (ReflectiveOperationException e) {
      System.err.println("Cannot access PrintStream.charOut. Cannot hook System.out/err:");
      e.printStackTrace(System.err);
    }
  }
}
