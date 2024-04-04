package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
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

      // track on the OutputStream level
      Field outField = FilterOutputStream.class.getDeclaredField("out");
      FileOutputStream fileOut = (FileOutputStream)
          Reflection.getFieldValue(Reflection.getFieldValue(printStream, outField), outField);
      FileDescriptor fd = fileOut.getFD();
      FileDescriptorTrackerRepository.createTracker(fd, false, true,
          TrackerTree.node("System").node(name));

      // track on the OutputStreamWriter level
      OutputStreamWriter writer =
          (OutputStreamWriter) Reflection.getFieldValue(printStream, charOutField);
      OutputStreamWriterHook.createOutputStreamWriterTracker(writer, fileOut);
    } catch (ReflectiveOperationException | IOException e) {
      System.err.println("Cannot access PrintStream.charOut. Cannot hook System.out/err:");
      e.printStackTrace(System.err);
    }
  }
}
