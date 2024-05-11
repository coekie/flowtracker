package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileOutputStreamHook {

  @Hook(target = "java.io.FileOutputStream",
      method = "void <init>(java.io.File,boolean)")
  public static void afterInit(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") File file) {
    if (Trackers.isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd, false, true,
          TrackerTree.fileNode(file.getAbsolutePath()));
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
        tracker.setSource(tracker.getLength(), 1, sourcePoint);
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
