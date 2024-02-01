package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.TrackerUpdater;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ByteBufferHook {
  private static final Field byteBufferHb =
      Reflection.getDeclaredField(ByteBuffer.class, "hb");
  private static final Field byteBufferOffset =
      Reflection.getDeclaredField(ByteBuffer.class, "offset");

  @Hook(target = "java.nio.ByteBuffer",
      condition = "version > 11",
      method = "void putBuffer(int,java.nio.ByteBuffer,int,int)")
  public static void afterPutBuffer(@Arg("THIS") ByteBuffer target, @Arg("ARG0") int pos,
      @Arg("ARG1") ByteBuffer src, @Arg("ARG2") int srcPos, @Arg("ARG3") int n) {
    if (target.isDirect()) {
      return;
    }

    byte[] targetArray = (byte[]) Reflection.getFieldValue(target, byteBufferHb);
    int targetOffset = Reflection.getInt(target, byteBufferOffset);


    // note: when it's a direct buffer then srcArray is null, and that's fine: we just don't track
    // that yet.
    byte[] srcArray = (byte[]) Reflection.getFieldValue(src, byteBufferHb);
    int srcOffset = Reflection.getInt(src, byteBufferOffset);

    // alternative: we could look at the address that SCOPED_MEMORY_ACCESS.copyMemory is being
    // called with, and from there calculate what offset into the array we're reading from and
    // writing to
    TrackerUpdater.setSource(targetArray, targetOffset + pos, n, srcArray, srcOffset + srcPos);
  }

  @Hook(target = "java.nio.DirectByteBuffer",
      condition = "version < 17",
      method = "java.nio.ByteBuffer get(byte[],int,int)")
  public static void afterDirectBufferGet(@Arg("ARG0") byte[] target,
      @Arg("ARG1") int offset, @Arg("ARG2") int length) {
    TrackerUpdater.setSource(target, offset, length, null, -1);
  }
}
