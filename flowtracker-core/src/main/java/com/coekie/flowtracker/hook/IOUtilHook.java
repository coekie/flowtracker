package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import java.io.FileDescriptor;
import java.nio.ByteBuffer;

// IOUtil is used by e.g. FileChannelImpl. This is tested in FileChannelTest.
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class IOUtilHook {

  @Hook(target = "sun.nio.ch.IOUtil",
      condition = "version >= 17", // between JDK 11 and 17 an extra argument "async" was added
      method = "int read(java.io.FileDescriptor,java.nio.ByteBuffer,long,boolean,boolean,int,sun.nio.ch.NativeDispatcher)")
  @Hook(target = "sun.nio.ch.IOUtil",
      condition = "version < 17",
      method = "int read(java.io.FileDescriptor,java.nio.ByteBuffer,long,boolean,int,sun.nio.ch.NativeDispatcher)")
  public static void afterReadByteBuffer(@Arg("RETURN") int result, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") ByteBuffer dst, @Arg("ARG2") long position) {
    // TODO do something with position (seeking)
    ByteOriginTracker fdTracker = FileDescriptorTrackerRepository.getReadTracker(fd);
    if (fdTracker != null && result > 0 && !dst.isDirect()) {
      TrackerUpdater.setSourceTracker(dst.array(), dst.position() - result, result, fdTracker,
          fdTracker.getLength());
      fdTracker.append(dst.array(), dst.position() - result, result);
    }
  }

  @Hook(target = "sun.nio.ch.IOUtil",
      condition = "version >= 17", // between JDK 11 and 17 an extra argument "async" was added
      method = "int write(java.io.FileDescriptor,java.nio.ByteBuffer,long,boolean,boolean,int,sun.nio.ch.NativeDispatcher)")
  @Hook(target = "sun.nio.ch.IOUtil",
      condition = "version < 17",
      method = "int write(java.io.FileDescriptor,java.nio.ByteBuffer,long,boolean,int,sun.nio.ch.NativeDispatcher)")
  public static void afterWriteByteBuffer(@Arg("RETURN") int result, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") ByteBuffer src, @Arg("ARG2") long position) {
    // TODO do something with position (seeking)
    ByteSinkTracker fdTracker = FileDescriptorTrackerRepository.getWriteTracker(fd);
    if (fdTracker != null && result > 0 && !src.isDirect()) {
      Tracker srcTracker = TrackerRepository.getTracker(src.array());
      if (srcTracker != null) {
        fdTracker.setSource(fdTracker.getLength(), result, srcTracker, src.position() - result);
      }
      fdTracker.append(src.array(), src.position() - result, result);
    }
  }
}
