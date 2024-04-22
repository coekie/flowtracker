package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.CharOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
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
import java.util.Map.Entry;

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
    addEnvTracking();
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

  /** Track environment variables */
  private static void addEnvTracking() {
    CharOriginTracker tracker = new CharOriginTracker();
    tracker.addTo(TrackerTree.node("System").node("env"));
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : System.getenv().entrySet()) {
      appendAndSetSource(tracker, entry.getKey());
      tracker.append('=');
      // reduce the chance of accidentally leaking secrets in environment variables: redact them.
      // (you shouldn't share access to flowtracker ui or snapshots for a process that has secrets
      // in memory to someone/somewhere those secrets shouldn't leak in the first place... but let's
      // just make that a bit less unsafe anyway. this obviously won't catch all secrets.)
      if (entry.getKey().contains("SECRET") || entry.getKey().contains("TOKEN")) {
        appendAndSetSource(tracker, "*".repeat(entry.getValue().length()));
      } else {
        appendAndSetSource(tracker, entry.getValue());
      }
      tracker.append('\n');
    }
  }

  /** Append `str` to `origin`, and set `origin` as its source */
  private static void appendAndSetSource(CharOriginTracker origin, String str) {
    int sourceIndex = origin.getLength();
    origin.append(str);
    DefaultTracker tracker = new DefaultTracker();
    tracker.setSource(0, str.length(), origin, sourceIndex);
    StringHook.setStringTracker(str, tracker);
  }
}
