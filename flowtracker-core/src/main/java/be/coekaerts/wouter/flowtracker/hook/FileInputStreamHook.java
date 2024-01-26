package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileInputStreamHook {

  @Hook(target = "java.io.FileInputStream",
      method = "void <init>(java.io.File)")
  public static void afterInit(@Arg("FileInputStream_fd") FileDescriptor fd,
      @Arg("ARG0") File file) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "FileInputStream for " + file.getAbsolutePath(),
          true, false);
    }
  }

  // note: there is no hook for the constructor that takes a FileDescriptor

  @Hook(target = "java.io.FileInputStream",
      method = "int read()")
  public static void afterRead1(@Arg("RETURN") int result,
      @Arg("FileInputStream_fd") FileDescriptor fd, @Arg("INVOCATION") Invocation invocation) {
    if (result > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        Invocation.returning(invocation, TrackerPoint.of(tracker, tracker.getLength()));
        tracker.append((byte) result);
      }
    }
  }

  @Hook(target = "java.io.FileInputStream",
      method = "int read(byte[])")
  public static void afterReadByteArray(@Arg("RETURN") int read,
      @Arg("FileInputStream_fd") FileDescriptor fd, @Arg("ARG0") byte[] buf) {
    if (read > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, 0, read, tracker, tracker.getLength());
        tracker.append(buf, 0, read);
      }
    }
  }

  @Hook(target = "java.io.FileInputStream",
      method = "int read(byte[],int,int)")
  public static void afterReadByteArrayOffset(@Arg("RETURN") int read,
      @Arg("FileInputStream_fd") FileDescriptor fd, @Arg("ARG0") byte[] buf,
      @Arg("ARG1") int offset) {
    if (read > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, offset, read, tracker, tracker.getLength());
        tracker.append(buf, offset, read);
      }
    }
  }
}
