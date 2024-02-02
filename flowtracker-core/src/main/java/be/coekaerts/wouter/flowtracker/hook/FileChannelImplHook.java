package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.FileDescriptorTrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileChannelImplHook {
  // for JDK<=17
  @Hook(target = "sun.nio.ch.FileChannelImpl",
      method = "void <init>(java.io.FileDescriptor,java.lang.String,boolean,boolean,boolean,java.lang.Object)")
  // for JDK>=21
  @Hook(target = "sun.nio.ch.FileChannelImpl",
      method = "void <init>(java.io.FileDescriptor,java.lang.String,boolean,boolean,boolean,java.io.Closeable)")
  public static void afterInit(@Arg("FileChannelImpl_fd") FileDescriptor fd,
      @Arg("ARG1") String path, @Arg("ARG2") boolean readable, @Arg("ARG3") boolean writeable) {
    if (Trackers.isActive()) {
      if (!FileDescriptorTrackerRepository.hasTracker(fd)) {
        FileDescriptorTrackerRepository.createTracker(fd, "FileChannel for " + path, readable,
            writeable, TrackerTree.fileNode(path));
      }
    }
  }
}
