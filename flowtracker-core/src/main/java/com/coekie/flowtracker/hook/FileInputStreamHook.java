package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import com.coekie.flowtracker.tracker.Trackers;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileInputStreamHook {

  @Hook(target = "java.io.FileInputStream",
      method = "void <init>(java.io.File)")
  public static void afterInit(@Arg("FileInputStream_fd") FileDescriptor fd,
      @Arg("ARG0") File file) {
    if (Trackers.isActive() && !ClassLoaderHook.shouldHideFileReading(file.getPath())) {
      FileDescriptorTrackerRepository.createTracker(fd, true, false,
          TrackerTree.fileNode(file.getAbsolutePath()));
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
