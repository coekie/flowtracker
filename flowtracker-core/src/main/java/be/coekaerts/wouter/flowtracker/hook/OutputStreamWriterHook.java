package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

/**
 * Hook methods called by instrumented code for OutputStreamWriter.
 *
 * <p>This is partially redundant with tracking the content of FileOutputStream.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class OutputStreamWriterHook {
  private static final Field filterOutputStreamOut =
      Reflection.getDeclaredField(FilterOutputStream.class, "out");

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream)")
  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream,java.lang.String)")
  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream,java.nio.charset.Charset)")
  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream,java.nio.charset.CharsetEncoder)")
  public static void afterInit(@Arg("THIS") OutputStreamWriter target,
      @Arg("ARG0") OutputStream stream) {
    if (Trackers.isActive()) {
      createOutputStreamWriterTracker(target, stream);
    }
  }

  static void createOutputStreamWriterTracker(OutputStreamWriter target,
      OutputStream stream) {
    Tracker streamTracker = getOutputStreamTracker(stream);
    CharSinkTracker tracker = new CharSinkTracker();
    tracker.addTo(TrackerTree.nodeOrUnknown(streamTracker).node("Writer"));
    TrackerRepository.setTracker(target, tracker);
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(int)")
  public static void afterWrite1(@Arg("THIS") OutputStreamWriter target, @Arg("ARG0") int c,
    @Arg("INVOCATION") Invocation invocation) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      TrackerPoint sourcePoint = Invocation.getArgPoint(invocation, 0);
      if (sourcePoint != null) {
        tracker.setSource(tracker.getLength(), 1, sourcePoint.tracker, sourcePoint.index);
      }
      ((CharSinkTracker) tracker).append((char) c);
    }
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(char[],int,int)")
  public static void afterWriteCharArrayOffset(@Arg("THIS") OutputStreamWriter target,
      @Arg("ARG0") char[] cbuf, @Arg("ARG1") int off, @Arg("ARG2") int len) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      Tracker sourceTracker = TrackerRepository.getTracker(cbuf);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((CharSinkTracker) tracker).append(cbuf, off, len);
    }
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(java.lang.String,int,int)")
  public static void afterWriteStringOffset(@Arg("THIS") OutputStreamWriter target,
      @Arg("ARG0") String str, @Arg("ARG1") int off, @Arg("ARG2") int len) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      Tracker sourceTracker = StringHook.getStringTracker(str);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((CharSinkTracker) tracker).append(str, off, len);
    }
  }

  private static Tracker getOutputStreamTracker(OutputStream os) {
    if (os instanceof FileOutputStream) {
      try {
        return FileDescriptorTrackerRepository.forceGetWriteTracker(((FileOutputStream) os).getFD());
      } catch (IOException e) {
        return null;
      }
    } else if (os instanceof FilterOutputStream) {
      return getOutputStreamTracker(
          (OutputStream) Reflection.getFieldValue(os, filterOutputStreamOut));
    }
    return null;
  }
}
