package be.coekaerts.wouter.flowtracker.hook;

import java.io.FileDescriptor;

/**
 * Hooks for java.net.SocketInputStream, for JDK 11.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class NetSocketInputStreamHook {
  public static void afterSocketRead(int read, FileDescriptor fd, byte[] buf, int offset) {
    FileInputStreamHook.afterReadByteArrayOffset(read, fd, buf, offset);
  }
}
