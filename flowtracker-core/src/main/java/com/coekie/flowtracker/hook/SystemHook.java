package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.tracker.CharOriginTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import com.coekie.flowtracker.util.Config;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SystemHook {
  @SuppressWarnings("SuspiciousSystemArraycopy")
  public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
    System.arraycopy(src, srcPos, dest, destPos, length);
    TrackerUpdater.setSource(dest, destPos, length, src, srcPos);
  }

  public static void initialize(Config config) {
    // we have to add trackers to the existing streams, because they were created before flowtracker
    // was initialized.
    addSystemOutErrTracker(System.out, "System.out", config);
    addSystemOutErrTracker(System.err, "System.err", config);
    addEnvTracking();
    addPropertiesTracking();
  }

  private static void addSystemOutErrTracker(PrintStream printStream, String name, Config config) {
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
      if (OutputStreamWriterHook.enabled(config)) {
        OutputStreamWriter writer =
            (OutputStreamWriter) Reflection.getFieldValue(printStream, charOutField);
        OutputStreamWriterHook.createOutputStreamWriterTracker(writer, fileOut);
      }
    } catch (ReflectiveOperationException | IOException e) {
      System.err.println("Cannot access PrintStream.charOut. Cannot hook System.out/err:");
      e.printStackTrace(System.err);
    }
  }

  /** Track environment variables */
  private static void addEnvTracking() {
    // a tracker that has as content a dump of all environment variables in "key=value\n" format.
    // we pretend that the environment variables were read from that content.
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
    StringHook.setStringSource(str, origin, sourceIndex);
  }

  /** Track system properties */
  private static void addPropertiesTracking() {
    // tracking of properties works a bit different from environment variables, because the values
    // for properties could have come from different places; the same String instances might be used
    // elsewhere too (e.g. they could be interned Strings), so pointing all those existing system
    // property value Strings to the System properties tracker would give incorrect results (~false
    // positives).
    // unlike environment variables, system properties are mutable, so we make tracked copies of the
    // keys and values, and replace them with that.

    // a tracker that has as content a dump of all system properties in "key=value\n" format.
    // we pretend that the properties were read from that content.
    CharOriginTracker tracker = new CharOriginTracker();
    tracker.addTo(TrackerTree.node("System").node("properties"));

    Properties props = System.getProperties();
    synchronized (props) {
      List<Object> keys = new ArrayList<>(props.keySet());
      for (Object key : keys) {
        Object value = props.get(key);
        // unlikely, but Properties can get polluted with non-Strings.
        if (!(key instanceof String && value instanceof String)) {
          continue;
        }
        String keyCopy = new String(((String) key).getBytes());
        String valueCopy = new String(((String) value).getBytes());
        appendAndSetSource(tracker, keyCopy);
        tracker.append('=');
        appendAndSetSource(tracker, valueCopy);
        tracker.append('\n');
        props.remove(key); // remove before overriding so that it keeps our instance of the key
        props.put(keyCopy, valueCopy);
      }
    }
  }
}
