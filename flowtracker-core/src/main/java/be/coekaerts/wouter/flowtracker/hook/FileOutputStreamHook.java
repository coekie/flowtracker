package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileOutputStreamHook {

  @Hook(target = "java.io.FileOutputStream",
      method = "void <init>(java.io.File,boolean)")
  public static void afterInit(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") File file) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd,
          "FileOutputStream for " + file.getAbsolutePath(),
          false, true, TrackerTree.fileNode(file.getAbsolutePath()));
    }
  }

  @Hook(target = "java.io.FileOutputStream",
      method = "void write(int)")
  public static void afterWrite1(@Arg("FileOutputStream_fd") FileDescriptor fd, @Arg("ARG0") int c,
      @Arg("INVOCATION") Invocation invocation) {
    ByteSinkTracker tracker = FileDescriptorTrackerRepository.getWriteTracker(fd);
    if (tracker != null) {
      TrackerPoint sourcePoint = Invocation.getArgPoint(invocation, 0);
      if (sourcePoint != null) {
        tracker.setSource(tracker.getLength(), 1, sourcePoint.tracker, sourcePoint.index);
      }
      tracker.append((byte) c);
    }
  }

  @Hook(target = "java.io.FileOutputStream",
      method = "void write(byte[])")
  public static void afterWriteByteArray(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") byte[] buf) {
    afterWriteByteArrayOffset(fd, buf, 0, buf.length);
  }

  @Hook(target = "java.io.FileOutputStream",
      method = "void write(byte[],int,int)")
  public static void afterWriteByteArrayOffset(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") byte[] buf, @Arg("ARG1") int off, @Arg("ARG2") int len) {
    ByteSinkTracker tracker = FileDescriptorTrackerRepository.getWriteTracker(fd);
    if (tracker != null) {
      Tracker sourceTracker = TrackerRepository.getTracker(buf);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      tracker.append(buf, off, len);
    }
  }
}
