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

import com.coekie.flowtracker.hook.StringConcatFactoryHook;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

/**
 * Handles instrumentation of an invokedynamic instruction to StringConcatFactory.
 * <p>
 * This instruments that call in two ways:<ul>
 *   <li>Replace string constants with constantsTransformation.stringConstantDynamic. That way e.g.
 *    String constants that are part of the concatenation (that end up in the recipe) are tracked.
 *    This is similar to what {@link StringLdc} does for other String literals.
 *   <li>When there are (non-constant) char values passed into the concatenation (e.g.
 *     `"foo" + someChar`), then the call to StringConcatFactory is replaced with a call to our hook
 *     method, passing in the source of the char values as {@link TrackerPoint}s.
 *     The goal of that is similar to how {@link InvocationArgStore} handles it for char values
 *     passed into other method calls (but it's implemented very differently).
 * </ul>
 */
public class StringConcatenation extends Store {
  /**
   * Handle for {@link
   * StringConcatFactory#makeConcatWithConstants(Lookup, String, MethodType, String, Object...)}
   */
  static final Handle realMakeConcatWithConstants =
      new Handle(Opcodes.H_INVOKESTATIC,
          "java/lang/invoke/StringConcatFactory",
          "makeConcatWithConstants",
          "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
              + "Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
          false);
  /**
   * Handle for {@link
   * StringConcatFactoryHook#makeConcatWithConstants(Lookup, String, MethodType, String, String, Object...)}
   */
  private static final Handle hookedMakeConcatWithConstants =
      new Handle(Opcodes.H_INVOKESTATIC,
          "com/coekie/flowtracker/hook/StringConcatFactoryHook",
          "makeConcatWithConstants",
          "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
              + "Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)"
              + "Ljava/lang/invoke/CallSite;",
          false);

  private final InvokeDynamicInsnNode insn;

  /**
   * Arguments of type char that can be tracked. Untracked (e.g. non-char) args are null in this
   * array. If there are no tracked char args, then the array is null.
   */
  private final FlowValue[] trackableCharArgs;

  private StringConcatenation(InvokeDynamicInsnNode insn, FlowFrame frame) {
    super(frame);
    this.insn = insn;

    // find trackableCharArgs
    FlowValue[] trackableCharArgs = null;
    Type[] argTypes = Type.getArgumentTypes(insn.desc);
    for (int i = 0; i < argTypes.length; i++) {
      if (Type.CHAR_TYPE.equals(argTypes[i])) {
        FlowValue value = getStackFromTop(argTypes.length - i - 1);
        if (value.isTrackable()) {
          if (trackableCharArgs == null) {
            trackableCharArgs = new FlowValue[argTypes.length];
          }
          trackableCharArgs[i] = value;
        }
      }
    }
    this.trackableCharArgs = trackableCharArgs;
  }

  @Override
  void instrument(FlowMethodAdapter methodNode) {
    ConstantsTransformation constantsTransformation = methodNode.constantsTransformation;

    // StringConcatFactory was introduced in jdk 9, condy in 11. for simplicity, we don't handle
    // tracking of string concatenation for classes compiled with jdk 9 or 10 (with
    // StringConcatFactory but without condy available).
    // just like in StringLdc, we replace the String constants being loaded with tracked Strings.
    // note: this isn't even StringConcatFactory-specific, if we wanted we could do this for all
    // invokedynamic instructions. but I haven't seen an example yet of where that's useful.
    if (methodNode.canUseConstantDynamic()) {
      Object[] bsmArgs = insn.bsmArgs;
      for (int i = 0; i < bsmArgs.length; i++) {
        if (bsmArgs[i] instanceof String) {
          String argValue = (String) bsmArgs[i];
          int offset = constantsTransformation.trackConstantString(methodNode, argValue);
          bsmArgs[i] = constantsTransformation.stringConstantDynamic(offset, argValue);
        }
      }
    }

    if (trackableCharArgs != null) {
      // insert code to load the TrackerPoints
      InsnList toInsert = new InsnList();
      // the mask, to indicate which parameters are tracked, see StringConcatFactoryHook.parseMask
      char[] mask = new char[trackableCharArgs.length];
      int trackedCount = 0;
      for (int i = 0; i < trackableCharArgs.length; i++) {
        mask[i] = (trackableCharArgs[i] == null) ? '.' : 'T';
        if (trackableCharArgs[i] != null) {
          trackableCharArgs[i].ensureTracked();
          trackableCharArgs[i].loadSourcePoint(toInsert);
          trackedCount++;
        }
      }
      methodNode.instructions.insertBefore(insn, toInsert);
      methodNode.maxStack = Math.max(methodNode.maxStack, frame.fullStackSize() + trackedCount + 1);

      // update signature of called method handle: add the TrackerPoint parameters
      insn.desc = insn.desc.replace(")",
          "Lcom/coekie/flowtracker/tracker/TrackerPoint;".repeat(trackedCount) + ')');

      // call our hook method instead of the real makeConcatWithConstants
      insn.bsm = hookedMakeConcatWithConstants;

      // insert the mask as second bootstrap method argument
      Object[] newBsmArgs = new Object[insn.bsmArgs.length + 1];
      newBsmArgs[0] = insn.bsmArgs[0]; // recipe is still the first argument
      newBsmArgs[1] = new String(mask);
      // copy over other constants (the "Object... constants" of makeConcatWithConstants)
      System.arraycopy(insn.bsmArgs, 1, newBsmArgs, 2, insn.bsmArgs.length - 1);
      insn.bsmArgs = newBsmArgs;
    }
  }

  /** Add a {@link StringConcatenation} to `toInstrument` when we need to instrument it */
  static void analyze(List<Instrumentable> toInstrument, InvokeDynamicInsnNode idInsn,
      FlowFrame frame) {
    if (idInsn.bsm.equals(StringConcatenation.realMakeConcatWithConstants)) {
      toInstrument.add(new StringConcatenation(idInsn, frame));
    }
  }
}
