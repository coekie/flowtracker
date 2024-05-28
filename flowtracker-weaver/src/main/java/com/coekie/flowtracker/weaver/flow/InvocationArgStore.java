package com.coekie.flowtracker.weaver.flow;

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

import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * The passing of a value as argument into the invocation of another method that may be instrumented
 * with {@link Invocation}.
 */
class InvocationArgStore extends Store {
  // Two reasons we don't want to instrument arguments beyond this:
  // - the ICONST_0 + i doesn't work for higher ones (we could fix that by using LdcInsNode)
  // - in Invocation we have getArg0Tracker/getArg0Index methods up to arg 5
  // - methods with more arguments than that are probably not as likely to be worth instrumenting
  static final int MAX_ARG_NUM_TO_INSTRUMENT = 5;

  // for now, we only support calls with one argument
  private final FlowValue[] args;
  private final InvocationOutgoingTransformation transformation;

  private InvocationArgStore(MethodInsnNode mInsn, FlowFrame frame, FlowFrame nextFrame) {
    super(frame);
    boolean[] toInstrument = requireNonNull(argsToInstrument(mInsn.owner, mInsn.name, mInsn.desc));
    this.args = new FlowValue[toInstrument.length];
    for (int i = 0; i < args.length; i++) {
      if (toInstrument[i]) {
        args[i] = getStackFromTop(args.length - i - 1);
      }
    }
    this.transformation = getOrCreateOutgoingTransformation(mInsn, frame, nextFrame);
  }

  /**
   * Get the {@link InvocationOutgoingTransformation} created by the {@link InvocationReturnValue}
   * for the same call if it exists; otherwise create a new one.
   */
  private static InvocationOutgoingTransformation getOrCreateOutgoingTransformation(
      MethodInsnNode mInsn, FlowFrame frame, FlowFrame nextFrame) {
    if (nextFrame != null && !mInsn.desc.endsWith(")V")) {
      FlowValue value = nextFrame.getStack(nextFrame.getStackSize() - 1);
      if (value instanceof InvocationReturnValue) {
        InvocationReturnValue invocationReturnValue = ((InvocationReturnValue) value);
        return invocationReturnValue.transformation;
      }
    }
    return new InvocationOutgoingTransformation(mInsn, frame.getMethod());
  }

  @Override
  void instrument(FlowMethod methodNode) {
    if (!anyArgIsTrackable()) {
      return;
    }

    // we add these instructions using insertInvocationPreparation, so Invocation is on top of the
    // stack. setArg returns the Invocation, so it's on top of the stack again.
    InsnList toInsert = new InsnList();
    methodNode.addComment(toInsert, "begin InvocationArgStore.insertTrackStatements");

    for (int i = 0; i < args.length; i++) {
      FlowValue arg = args[i];
      // if we know where the value passed in as argument came from
      if (arg != null && arg.isTrackable()) {
        arg.ensureTracked();
        arg.loadSourcePoint(toInsert, this);
        toInsert.add(
            new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "com/coekie/flowtracker/tracker/Invocation",
                "setArg" + i,
                "(Lcom/coekie/flowtracker/tracker/TrackerPoint;)"
                    + "Lcom/coekie/flowtracker/tracker/Invocation;"));
      }
    }
    methodNode.addComment(toInsert, "end InvocationArgStore.insertTrackStatements");

    transformation.ensureInstrumented();
    transformation.insertInvocationPreparation(toInsert);

    // +3: 1 for the Invocation + 2 for loadSourcePoint
    methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);
  }

  private boolean anyArgIsTrackable() {
    for (FlowValue arg : args) {
      if (arg != null && arg.isTrackable()) {
        return true;
      }
    }
    return false;
  }

  static boolean shouldInstrumentInvocationArg(String owner, String name, String desc) {
    return argsToInstrument(owner, name, desc) != null;
  }

  /** Determines which arguments should be instrumented. null if none of them should be. */
  static boolean[] argsToInstrument(String owner, String name, String desc) {
    Type[] args = Type.getArgumentTypes(desc);
    if (args.length > MAX_ARG_NUM_TO_INSTRUMENT + 1 || name.equals("<init>")) {
      return null;
    }

    boolean[] result = new boolean[args.length];
    boolean any = false;

    switch (owner) {
      case "java/io/Bits": // in JDK < 21
        if (name.equals("putInt")) {
          result[2] = true; // the `value` argument
          return result;
        }
        break;
      case "jdk/internal/util/ByteArray": // used in ByteArrayHook
        if (name.equals("setUnsignedShort") || name.equals("setInt")) {
          result[2] = true; // the `value` argument
          return result;
        }
        break;
      case "java/nio/ByteBuffer": // used in ByteBufferHook
        if (name.equals("putInt")) {
          result[result.length - 1] = true; // the `value` argument, for both overloads
          return result;
        }
        break;
    }

    boolean eager = name.contains("write") || name.contains("Write") || name.contains("print");

    for (int i = 0; i < args.length; i++) {
      Type arg = args[i];
      // instrument all char, byte and short args, because they're very likely to be relevant,
      // but for int arguments only if we're eager to because the name of the method suggests it
      // might be relevant.
      if (arg.getSort() == Type.CHAR || arg.getSort() == Type.BYTE || arg.getSort() == Type.SHORT
          || (eager && arg.getSort() == Type.INT)) {
        result[i] = true;
        any = true;
      }
    }
    return any ? result : null;
  }

  /** Add a {@link InvocationArgStore} to `toInstrument` when we need to instrument it */
  static void analyze(List<Instrumentable> toInstrument, MethodInsnNode mInsn, FlowFrame frame,
      Frame<FlowValue>[] frames, int insnIndex) {
    if (InvocationArgStore.shouldInstrumentInvocationArg(mInsn.owner, mInsn.name, mInsn.desc)) {
      toInstrument.add(new InvocationArgStore(mInsn, frame,
          // next frame, might contain the return value of the call
          insnIndex + 1 < frames.length ? (FlowFrame) frames[insnIndex + 1] : null));
    }
  }
}
