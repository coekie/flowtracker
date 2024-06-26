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
import com.coekie.flowtracker.annotation.HookLocation;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import com.coekie.flowtracker.util.ConcurrentWeakIdentityHashMap;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/**
 * Tracks the input going into a {@link Deflater} and its output, in two separate trackers.
 * No attempt is made to track exactly which uncompressed bytes particular compressed bytes
 * correspond to.
 */
public class DeflaterHook {
  private static final ConcurrentWeakIdentityHashMap<Deflater, DeflaterState> stateMap
      = new ConcurrentWeakIdentityHashMap<>();

  private static final VarHandle deflater_input =
      Reflection.varHandle(Deflater.class, "input", ByteBuffer.class);
  private static final VarHandle deflater_inputArray =
      Reflection.varHandle(Deflater.class, "inputArray", byte[].class);
  private static final VarHandle deflater_inputPos =
      Reflection.varHandle(Deflater.class, "inputPos", int.class);

  @Hook(
      target = "java/util/zip/Deflater",
      method = "int deflate(byte[], int, int, int)",
      location = HookLocation.ON_ENTER)
  @Hook(
      target = "java/util/zip/Deflater",
      method = "int deflate(java.nio.ByteBuffer, int)",
      location = HookLocation.ON_ENTER)
  public static void beforeDeflate(@Arg("THIS") Deflater deflater) {
    // remember where we were in the input before deflate was called, so that we know afterward
    // how much of the input was consumed
    DeflaterState state = stateMap.computeIfAbsent(deflater, d -> new DeflaterState());
    ByteBuffer input = (ByteBuffer) deflater_input.get(deflater);
    if (input != null) {
      state.inputPosBeforeDeflate = input.position();
    } else {
      state.inputPosBeforeDeflate = (int) deflater_inputPos.get(deflater);
    }
  }

  @Hook(
      target = "java/util/zip/Deflater",
      method = "int deflate(byte[], int, int, int)")
  public static void afterDeflateBytes(@Arg("RETURN") int written, @Arg("THIS") Deflater deflater,
      @Arg("ARG0") byte[] output, @Arg("ARG1") int outputOffset) {
    DeflaterState state = stateMap.get(deflater);
    Context context = context();
    updateSinkAfterDeflate(context, deflater, state);
    updateOriginAfterDeflate(context, state, output, outputOffset, written);
  }

  @Hook(
      target = "java/util/zip/Deflater",
      method = "int deflate(java.nio.ByteBuffer, int)")
  public static void afterDeflateBuffer(@Arg("RETURN") int written, @Arg("THIS") Deflater deflater,
      @Arg("ARG0") ByteBuffer outputBuffer) {
    DeflaterState state = stateMap.get(deflater);
    Context context = context();
    updateSinkAfterDeflate(context, deflater, state);

    if (!outputBuffer.isDirect()) {
      byte[] output = ByteBufferHook.hb(outputBuffer);
      int outputOffset = ByteBufferHook.offset(outputBuffer) + outputBuffer.position() - written;
      updateOriginAfterDeflate(context, state, output, outputOffset, written);
    }
  }

  @Hook(
      target = "java/util/zip/Deflater",
      method = "void reset()")
  public static void afterReset(@Arg("THIS") Deflater deflater) {
    DeflaterState state = stateMap.get(deflater);
    if (state != null) {
      // use a new tracker for the next input and output.
      state.originTracker = null;
      state.sinkTracker = null;
    }
  }

  /**
   * Update the sink tracker, tracking the input that was deflated
   */
  private static void updateSinkAfterDeflate(Context context, Deflater deflater,
      DeflaterState state) {
    if (state.sinkTracker == null) {
      state.sinkTracker = new ByteSinkTracker();
    }

    ByteBuffer input = (ByteBuffer) deflater_input.get(deflater);
    if (input != null) {
      TrackerUpdater.appendByteBuffer(context, state.sinkTracker, input,
          state.inputPosBeforeDeflate, input.position() - state.inputPosBeforeDeflate);
    } else {
      byte[] inputArray = (byte[]) deflater_inputArray.get(deflater);
      int pos = (int) deflater_inputPos.get(deflater);
      TrackerUpdater.appendBytes(context, state.sinkTracker, inputArray,
          state.inputPosBeforeDeflate, pos - state.inputPosBeforeDeflate);
    }
  }

  /**
   * Update the origin tracker, tracking the deflated output
   */
  private static void updateOriginAfterDeflate(Context context, DeflaterState state, byte[] output,
      int outputOffset, int written) {
    if (state.originTracker == null) {
      state.originTracker = new ByteOriginTracker();
      state.originTracker.twin = state.sinkTracker;
      state.sinkTracker.twin = state.originTracker;
    }
    int originTrackerIndex = state.originTracker.getLength();
    state.originTracker.append(output, outputOffset, written);
    TrackerUpdater.setSourceTracker(context, output, outputOffset, written, state.originTracker,
        originTrackerIndex);
  }

  private static class DeflaterState {
    /** Position in the input before deflate was called */
    private int inputPosBeforeDeflate;
    private ByteSinkTracker sinkTracker;
    private ByteOriginTracker originTracker;
  }

  public static ByteSinkTracker getSinkTracker(Deflater deflater) {
    DeflaterState state = stateMap.get(deflater);
    return state == null ? null : state.sinkTracker;
  }

  public static ByteOriginTracker getOriginTracker(Deflater deflater) {
    DeflaterState state = stateMap.get(deflater);
    return state == null ? null : state.originTracker;
  }
}
