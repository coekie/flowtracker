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

import static org.objectweb.asm.Opcodes.ACC_STATIC;

import com.coekie.flowtracker.weaver.Types;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;

class FlowInterpreter extends Interpreter<FlowValue> {
  /** A BasicInterpreter that we delegate to, to figure out types */
  private final BasicInterpreter basicInterpreter = new BasicInterpreter(Opcodes.ASM9) {
    @Override
    public BasicValue newValue(Type type) {
      return FlowInterpreter.this.newValue(type);
    }
  };

  private FlowValue toFlowValue(BasicValue basicValue) {
    return basicValue == null ? null : newValue(basicValue.getType());
  }

  private final FlowMethod method;
  /** @see #initArgNumsToInstrument */
  private final int[] argNumsToInstrument;

  /**
   * Frame for which we are merging. That is, for which {@link #merge(FlowValue, FlowValue)}
   * is being called. Ideally that would be an argument for the merge method, but that's not how
   * asm works, so instead we set this field before merge is called.
   */
  private FlowFrame mergingFrame;

  FlowInterpreter(FlowMethod method) {
    super(Opcodes.ASM9);
    this.method = method;
    this.argNumsToInstrument = initArgNumsToInstrument(method);
  }

  @Override
  public FlowValue newValue(Type type) {
    if (type == null) {
      return UntrackableValue.UNINITIALIZED_VALUE;
    }

    // nit: this is inspired by BasicInterpreter, but we probably don't need all these cases
    //   (see e.g. how SourceInterpreter only uses type.getSize())
    switch (type.getSort()) {
      case Type.VOID:
        return UntrackableValue.VOID_VALUE;
      case Type.BOOLEAN:
      case Type.CHAR:
      case Type.BYTE:
      case Type.SHORT:
      case Type.INT:
        return UntrackableValue.INT_VALUE;
      case Type.FLOAT:
        return UntrackableValue.FLOAT_VALUE;
      case Type.LONG:
        return UntrackableValue.LONG_VALUE;
      case Type.DOUBLE:
        return UntrackableValue.DOUBLE_VALUE;
      case Type.OBJECT:
        // for String remember the exact type, because we use that in our analysis.
        if (Types.STRING.equals(type)) {
          return new UntrackableValue(type);
        } else {
          // for others the exact type doesn't matter, so don't bother (for performance: this makes
          // the analysis faster; asm's analyzer doesn't have to loop through the code as many times
          // when it learns that a value can have a different type)
          return UntrackableValue.REFERENCE_VALUE;
        }
      case Type.ARRAY:
        // for char[], byte[] remember the exact type, because we use those in our analysis.
        if (Types.CHAR_ARRAY.equals(type) || Types.BYTE_ARRAY.equals(type)) {
          return new UntrackableValue(type);
        } else {
          return UntrackableValue.REFERENCE_VALUE;
        }
      default:
        throw new AssertionError();
    }
  }

  @Override
  public FlowValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
    switch (insn.getOpcode()) {
      case Opcodes.ICONST_M1:
      case Opcodes.ICONST_0:
      case Opcodes.ICONST_1:
      case Opcodes.ICONST_2:
      case Opcodes.ICONST_3:
      case Opcodes.ICONST_4:
      case Opcodes.ICONST_5:
        return new ConstantValue(method, Type.INT_TYPE, insn,
            insn.getOpcode() - Opcodes.ICONST_0);
      case Opcodes.SIPUSH:
      case Opcodes.BIPUSH:
        return new ConstantValue(method, Type.INT_TYPE, insn,
            ((IntInsnNode) insn).operand);
      case Opcodes.LDC:
        LdcInsnNode ldcInsn = (LdcInsnNode) insn;
        if (ldcInsn.cst instanceof Integer) {
          return new ConstantValue(method, Type.INT_TYPE, insn, (Integer) ldcInsn.cst);
        } else if (ldcInsn.cst instanceof Long) {
          return new ConstantValue(method, Type.LONG_TYPE, insn, (Long) ldcInsn.cst);
        }
    }
    return toFlowValue(basicInterpreter.newOperation(insn));
  }

  @Override
  public FlowValue copyOperation(AbstractInsnNode insn, FlowValue value) {
    return new CopyValue(value, insn);
  }

  @Override
  public FlowValue unaryOperation(AbstractInsnNode insn, FlowValue value)
      throws AnalyzerException {
    switch (insn.getOpcode()) {
      case Opcodes.I2B:
      case Opcodes.I2C:
      case Opcodes.I2S:
        // cast operation. we don't care about the fact that the casting can change the value, so
        // we ignore that: we treat the result as the same value (same type, same source).
        // It's ok to keep it as the same type, because this is going from a BasicValue.INT_VALUE
        // (which includes byte, char & int) to another INT_VALUE.
        return value;
      case Opcodes.I2L:
        // cast that does change the type
        return new CastValue(Type.LONG_TYPE, insn, value);
      case Opcodes.L2I:
        // cast that does change the type
        return new CastValue(Type.INT_TYPE, insn, value);
      case Opcodes.GETFIELD:
        FieldInsnNode fieldInsn = (FieldInsnNode) insn;
        Type fieldType = Type.getType(((FieldInsnNode) insn).desc);
        if (FieldValue.shouldTrack(fieldType, fieldInsn)) {
          return new FieldValue(method, fieldInsn, fieldType);
        }
        break;
    }

    return toFlowValue(basicInterpreter.unaryOperation(insn, value));
  }

  @Override
  public FlowValue binaryOperation(AbstractInsnNode aInsn, FlowValue value1, FlowValue value2)
      throws AnalyzerException {
    switch (aInsn.getOpcode()) {
      case Opcodes.CALOAD: {
        return new ArrayLoadValue(method, (InsnNode) aInsn, Type.CHAR_TYPE);
      }
      case Opcodes.BALOAD: {
        return new ArrayLoadValue(method, (InsnNode) aInsn, Type.BYTE_TYPE);
      }
      case Opcodes.IALOAD: {
        return new ArrayLoadValue(method, (InsnNode) aInsn, Type.INT_TYPE);
      }
      case Opcodes.IAND:
      case Opcodes.LAND: {
        // treat `x & constant` as having the same source as x
        if (value2 instanceof ConstantValue) {
          return value1;
        } else if (value1 instanceof ConstantValue) {
          return value2;
        }
      }
      case Opcodes.IUSHR:
      case Opcodes.LUSHR: {
        // treat `x >>> constant` as having the same source as x.
        // for e.g. DataOutputStream.writeShort/writeChar, Bits.putShort/putInt (in older jdks)
        if (value2 instanceof ConstantValue) {
          return value1;
        }
      }
    }
    return toFlowValue(basicInterpreter.binaryOperation(aInsn, value1, value2));
  }

  @Override
  public FlowValue ternaryOperation(AbstractInsnNode insn, FlowValue value1, FlowValue value2,
      FlowValue value3) {
    return null;
  }

  @Override
  public FlowValue naryOperation(AbstractInsnNode insn, List<? extends FlowValue> values)
      throws AnalyzerException {
    if (insn instanceof MethodInsnNode) {
      MethodInsnNode mInsn = (MethodInsnNode) insn;
      if ("com/coekie/flowtracker/test/FlowTester".equals(mInsn.owner)) {
        if ("createSourceChar".equals(mInsn.name)
            || "createSourceShort".equals(mInsn.name)
            || "createSourceByte".equals(mInsn.name)
            || "createSourceInt".equals(mInsn.name)) {
          return new TesterValue(method, mInsn, false);
        } else if ("createSourceLong".equals(mInsn.name)) {
          return new TesterValue(method, mInsn, true);
        }
      } else if ("java/lang/Byte".equals(mInsn.owner) && "toUnsignedInt".equals(mInsn.name)) {
        return values.get(0);
      } else if ("java/lang/String".equals(mInsn.owner) && "charAt".equals(mInsn.name)) {
        return new CharAtValue(method, mInsn, false);
      } else if ("java/lang/CharSequence".equals(mInsn.owner) && "charAt".equals(mInsn.name)) {
        return new CharAtValue(method, mInsn, true);
      } else if (InvocationReturnValue.shouldInstrumentInvocation(mInsn.owner, mInsn.name, mInsn.desc)) {
        return new InvocationReturnValue(method, mInsn);
      }
    }
    return toFlowValue(basicInterpreter.naryOperation(insn, values));
  }

  @Override
  public void returnOperation(AbstractInsnNode insn, FlowValue value, FlowValue expected) {
  }

  @Override
  public FlowValue newReturnTypeValue(Type type) {
    if (type.getSort() == Type.VOID) {
      return null;
    }
    return super.newReturnTypeValue(type);
  }

  @Override
  public FlowValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
    if (argNumsToInstrument != null) {
      int argNum = argNumsToInstrument[local];
      if (argNum >= 0) {
        // we consider the first instruction of the method to be the instruction that created the
        // parameter values. that's not entirely correct (they get created _before_ that
        // instruction), but close enough.
        return new InvocationArgValue(method, method.instructions.getFirst(), argNum);
      }
    }
    return super.newParameterValue(isInstanceMethod, local, type);
  }

  /**
   * Returns an array indicating which locals contain which arguments to instrument.
   * The index in the array is the `local` (as in {@link #newParameterValue(boolean, int, Type)});
   * that is counting including the `this` argument and counting two for longs and doubles.
   * The value is the index of the argument if it is to be instrumented, otherwise -1.
   */
  private static int[] initArgNumsToInstrument(FlowMethod method) {
    boolean[] argsToInstrument =
        InvocationArgStore.argsToInstrument(method.owner, method.name, method.desc);
    if (argsToInstrument == null) {
      return null;
    }

    int argumentsSize = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
    boolean isInstanceMethod = (method.access & ACC_STATIC) == 0;
    int[] result = new int[argumentsSize + (isInstanceMethod ? 1 : 0)];

    // how we count currentLocal here is the same as in Analyzer.computeInitialFrame
    int currentLocal = 0;
    if (isInstanceMethod) {
      result[currentLocal++] = -1;
    }
    Type[] argumentTypes = Type.getArgumentTypes(method.desc);
    for (int i = 0; i < argumentTypes.length; i++) {
      Type argumentType = argumentTypes[i];
      result[currentLocal++] = argsToInstrument[i] ? i : -1;
      if (argumentType.getSize() == 2) {
        result[currentLocal++] = -1;
      }
    }
    return result;
  }

  void startMerge(FlowFrame mergingFrame) {
    this.mergingFrame = mergingFrame;
  }

  void endMerge() {
    this.mergingFrame = null;
  }

  @Override
  public FlowValue merge(FlowValue value1, FlowValue value2) {
    if (value1.equals(value2)) {
      return value1;
    } else {
      Type type1 = value1.getType();
      Type type2 = value2.getType();

      // TODO ideally we'd also do this if only one of the two is trackable. for that to work, we
      //  also need to know which instructions UntrackableValues came from
      if (value1.isTrackable() && value2.isTrackable()) {
        // if we can merge them in-place, do that
        FlowValue mergedInPlace = value1.mergeInPlace(value2);
        if (mergedInPlace != null) {
          return mergedInPlace;
        }

        // else, this is a "real" merge
        FlowValue merged = MergedValue.merge(mergeTypes(type1, type2), mergingFrame,
            value1, value2);
        if (merged != null) {
          return merged;
        }
      }

      // this is partially duplicating logic in mergeTypes; to avoid creating unnecessary extra
      // UntrackableValue instances
      if (type1 == null || type2 == null || type1.getSort() != type2.getSort()) {
        return UntrackableValue.UNINITIALIZED_VALUE;
      } else {
        switch (type1.getSort()) {
          case Type.INT:
            return UntrackableValue.INT_VALUE;
          case Type.FLOAT:
            return UntrackableValue.FLOAT_VALUE;
          case Type.LONG:
            return UntrackableValue.LONG_VALUE;
          case Type.DOUBLE:
            return UntrackableValue.DOUBLE_VALUE;
          case Type.OBJECT:
            return UntrackableValue.REFERENCE_VALUE;
          default:
            return UntrackableValue.UNINITIALIZED_VALUE;
        }
      }
    }
  }

  private Type mergeTypes(Type type1, Type type2) {
    if (type1 == null || type2 == null) {
      return null;
    } else if (type1.equals(type2)) {
      return type1;
    } else if (type1.getSort() == Type.OBJECT && type2.getSort() == Type.OBJECT) {
      return Types.OBJECT;
    } else {
      return null; // like FlowValue.UNINITIALIZED_VALUE
    }
  }
}
