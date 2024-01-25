package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Hooks for java.net.SocketOutputStream, for JDK 11.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class NetSocketOutputStreamHook {
  @Hook(target = "java.net.SocketOutputStream",
      condition = "version < 17",
      method = "void socketWrite(byte[],int,int)")
  public static void afterSocketWrite(@Arg("THIS") FileOutputStream os, @Arg("ARG0") byte[] buf,
      @Arg("ARG1") int off, @Arg("ARG2") int len)
      throws IOException {
    FileOutputStreamHook.afterWriteByteArrayOffset(os.getFD(), buf, off, len);
  }
}
