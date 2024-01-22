package be.coekaerts.wouter.flowtracker.hook;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Hooks for java.net.SocketOutputStream, for JDK 11.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class NetSocketOutputStreamHook {
  public static void afterSocketWrite(FileOutputStream os, byte[] buf, int off, int len)
      throws IOException {
    FileOutputStreamHook.afterWriteByteArrayOffset(os.getFD(), buf, off, len);
  }
}
