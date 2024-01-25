package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import java.io.FileDescriptor;

/**
 * Hooks for java.net.SocketInputStream, for JDK 11.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class NetSocketInputStreamHook {
  @Hook(target = "java.net.SocketInputStream",
      condition = "version < 17",
      method = "int socketRead(java.io.FileDescriptor,byte[],int,int,int)")
  public static void afterSocketRead(int read, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") byte[] buf, @Arg("ARG2") int offset) {
    FileInputStreamHook.afterReadByteArrayOffset(read, fd, buf, offset);
  }
}
