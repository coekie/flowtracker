package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SystemHook {
	@SuppressWarnings("SuspiciousSystemArraycopy")
	public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		Tracker.setSource(dest, destPos, length, src, srcPos);
	}

  public static void initialize() {
    // not really used
    TrackerRepository.createContentTracker(System.out).initDescriptor("System.out", null);
    TrackerRepository.createContentTracker(System.err).initDescriptor("System.err", null);
    TrackerRepository.createContentTracker(System.in).initDescriptor("System.in", null);

    // we have to add trackers to the existing streams, because they were created before flowtracker
    // was initialized.
    addSystemOutErrTracker(System.out, "System.out");
    addSystemOutErrTracker(System.err, "System.err");
  }

  private static void addSystemOutErrTracker(PrintStream printStream, String name) {
    try {
      Field charOutField = PrintStream.class.getDeclaredField("charOut");
      charOutField.setAccessible(true);
      OutputStreamWriterHook.createOutputStreamWriterTracker(
          (OutputStreamWriter) charOutField.get(printStream))
          .initDescriptor(name + ".charOut", null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      System.err.println("Cannot access PrintStream.charOut. Cannot hook System.out/err:");
      e.printStackTrace(System.err);
    }
  }
}
