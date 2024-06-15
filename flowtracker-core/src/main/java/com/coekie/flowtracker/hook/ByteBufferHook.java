package com.coekie.flowtracker.hook;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ByteBufferHook {
  private static final MethodHandle hbHandle =
      Reflection.getter(ByteBuffer.class, "hb", byte[].class);
  private static final MethodHandle offsetHandle =
      Reflection.getter(ByteBuffer.class, "offset", int.class);

  @Hook(target = "java.nio.ByteBuffer",
      condition = "version > 11",
      method = "void putBuffer(int,java.nio.ByteBuffer,int,int)")
  public static void afterPutBuffer(@Arg("THIS") ByteBuffer target, @Arg("ARG0") int pos,
      @Arg("ARG1") ByteBuffer src, @Arg("ARG2") int srcPos, @Arg("ARG3") int n) {
    if (target.isDirect()) {
      return;
    }

    byte[] targetArray = hb(target);
    int targetOffset = offset(target);


    // note: when it's a direct buffer then srcArray is null, and that's fine: we just don't track
    // that yet.
    byte[] srcArray = hb(src);
    int srcOffset = offset(src);

    // alternative: we could look at the address that SCOPED_MEMORY_ACCESS.copyMemory is being
    // called with, and from there calculate what offset into the array we're reading from and
    // writing to
    TrackerUpdater.setSource(context(), targetArray, targetOffset + pos, n, srcArray,
        srcOffset + srcPos);
  }

  @Hook(target = "java.nio.DirectByteBuffer",
      condition = "version < 17",
      method = "java.nio.ByteBuffer get(byte[],int,int)")
  public static void afterDirectBufferGet(@Arg("ARG0") byte[] target,
      @Arg("ARG1") int offset, @Arg("ARG2") int length) {
    TrackerUpdater.setSource(context(), target, offset, length, null, -1);
  }

  @Hook(target = "java.nio.HeapByteBuffer",
      method = "java.nio.ByteBuffer putChar(char)")
  public static void afterPutChar(@Arg("THIS") ByteBuffer target,
      @Arg("INVOCATION") Invocation invocation) {
    afterPutPrimitive(target, invocation, 2);
  }

  @Hook(target = "java.nio.HeapByteBuffer",
      method = "java.nio.ByteBuffer putChar(int,char)")
  public static void afterPutCharPosition(@Arg("THIS") ByteBuffer target, @Arg("ARG0") int pos,
      @Arg("INVOCATION") Invocation invocation) {
    afterPutPrimitivePosition(target, pos, invocation, 2);
  }

  @Hook(target = "java.nio.HeapByteBuffer",
      method = "java.nio.ByteBuffer putShort(short)")
  public static void afterPutShort(@Arg("THIS") ByteBuffer target,
      @Arg("INVOCATION") Invocation invocation) {
    afterPutPrimitive(target, invocation, 2);
  }

  @Hook(target = "java.nio.HeapByteBuffer",
      method = "java.nio.ByteBuffer putShort(int,short)")
  public static void afterPutShortPosition(@Arg("THIS") ByteBuffer target, @Arg("ARG0") int pos,
      @Arg("INVOCATION") Invocation invocation) {
    afterPutPrimitivePosition(target, pos, invocation, 2);
  }

  @Hook(target = "java.nio.HeapByteBuffer",
      method = "java.nio.ByteBuffer putInt(int)")
  public static void afterPutInt(@Arg("THIS") ByteBuffer target,
      @Arg("INVOCATION") Invocation invocation) {
    afterPutPrimitive(target, invocation, 4);
  }

  @Hook(target = "java.nio.HeapByteBuffer",
      method = "java.nio.ByteBuffer putInt(int,int)")
  public static void afterPutIntPosition(@Arg("THIS") ByteBuffer target, @Arg("ARG0") int pos,
      @Arg("INVOCATION") Invocation invocation) {
    afterPutPrimitivePosition(target, pos, invocation, 4);
  }

  /** Hook for putChar/putShort/putInt */
  private static void afterPutPrimitive(ByteBuffer target, Invocation invocation, int length) {
    byte[] targetArray = hb(target);
    int pos = target.position() - length; // position before we wrote the value
    int targetOffset = offset(target);
    TrackerUpdater.setSourceTrackerPoint(context(), targetArray, targetOffset + pos, length,
        Invocation.getArgPoint(invocation, 0));
  }

  /** Hook for putChar/putShort/putInt overload that takes the position as first argument */
  private static void afterPutPrimitivePosition(ByteBuffer target, int pos,
      Invocation invocation, int length) {
    byte[] targetArray = hb(target);
    int targetOffset = offset(target);
    TrackerUpdater.setSourceTrackerPoint(context(), targetArray, targetOffset + pos, length,
        Invocation.getArgPoint(invocation, 1));
  }

  static byte[] hb(ByteBuffer o) {
    try {
      return (byte[]) hbHandle.invokeExact(o);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  static int offset(ByteBuffer o) {
    try {
      return (int) offsetHandle.invokeExact(o);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }
}
