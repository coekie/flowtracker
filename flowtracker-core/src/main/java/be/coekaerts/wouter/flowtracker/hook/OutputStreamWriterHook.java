package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Hook methods called by instrumented code for OutputStreamWriter.
 *
 * <p>This is partially redundant with tracking the content of FileOutputStream.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class OutputStreamWriterHook {
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
      createOutputStreamWriterTracker(target).initDescriptor("OutputStreamWriter",
          TrackerRepository.getTracker(stream));
    }
  }

  static CharSinkTracker createOutputStreamWriterTracker(OutputStreamWriter target) {
    CharSinkTracker tracker = new CharSinkTracker();
    TrackerRepository.setInterestTracker(target, tracker);
    return tracker;
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(int)")
  public static void afterWrite1(@Arg("THIS") OutputStreamWriter target, @Arg("ARG0") int c) {
    Tracker tracker = TrackerRepository.getTracker(target);
    if (tracker != null) {
      ((CharSinkTracker) tracker).append((char) c);
      // TODO tracking of source of single character writes
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
}
