package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.Types;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
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

  private final FlowMethodAdapter flowMethodAdapter;

  /**
   * Frame for which we are merging. That is, for which {@link #merge(FlowValue, FlowValue)}
   * is being called. Ideally that would be an argument for the merge method, but that's not how
   * asm works, so instead we set this field before merge is called.
   */
  private FlowFrame mergingFrame;

  FlowInterpreter(FlowMethodAdapter flowMethodAdapter) {
    super(Opcodes.ASM9);
    this.flowMethodAdapter = flowMethodAdapter;
  }

  @Override
  public FlowValue newValue(Type type) {
    if (type == null) {
      return FlowValue.UNINITIALIZED_VALUE;
    }

    // nit: this is inspired by BasicInterpreter, but we probably don't need all these cases
    //   (see e.g. how SourceInterpreter only uses type.getSize())
    switch (type.getSort()) {
      case Type.VOID:
        return null;
      case Type.BOOLEAN:
      case Type.CHAR:
      case Type.BYTE:
      case Type.SHORT:
      case Type.INT:
        return FlowValue.INT_VALUE;
      case Type.FLOAT:
        return FlowValue.FLOAT_VALUE;
      case Type.LONG:
        return FlowValue.LONG_VALUE;
      case Type.DOUBLE:
        return FlowValue.DOUBLE_VALUE;
      case Type.ARRAY:
      case Type.OBJECT:
        // for char[] and byte[] remember the exact type
        if (Types.CHAR_ARRAY.equals(type) || Types.BYTE_ARRAY.equals(type)) {
          return new FlowValue(type);
        } else {
          // for others the exact type doesn't matter
          return FlowValue.REFERENCE_VALUE;
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
      case Opcodes.SIPUSH:
      case Opcodes.BIPUSH:
        return new ConstantValue(flowMethodAdapter, Type.INT_TYPE, flowMethodAdapter.owner, insn);
    }
    return toFlowValue(basicInterpreter.newOperation(insn));
  }

  @Override
  public FlowValue copyOperation(AbstractInsnNode insn, FlowValue value) {
    return value;
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
      case Opcodes.GETFIELD:
        FieldInsnNode fieldInsn = (FieldInsnNode) insn;
        Type fieldType = Type.getType(((FieldInsnNode) insn).desc);
        if (FieldValue.shouldTrackType(fieldType)) {
          return new FieldValue(flowMethodAdapter, fieldInsn, fieldType);
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
        InsnNode insn = (InsnNode) aInsn;
        return new ArrayLoadValue(flowMethodAdapter, insn, Type.CHAR_TYPE, Types.CHAR_ARRAY);
      }
      case Opcodes.BALOAD: {
        InsnNode insn = (InsnNode) aInsn;
        return new ArrayLoadValue(flowMethodAdapter, insn, Type.BYTE_TYPE, Types.BYTE_ARRAY);
      }
      case Opcodes.IAND: {
        // treat `x & constant` as having the same source as x
        if (value2 instanceof ConstantValue) {
          return value1;
        } else if (value1 instanceof ConstantValue) {
          return value2;
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
      if ("charAt".equals(mInsn.name) && "(I)C".equals(mInsn.desc)
          && ("java/lang/String".equals(mInsn.owner)
          || "java/lang/CharSequence".equals(mInsn.owner))) {
        return new CharAtValue(flowMethodAdapter, mInsn);
      } else if (InvocationReturnValue.shouldInstrumentInvocation(mInsn.name, mInsn.desc)) {
        return new InvocationReturnValue(flowMethodAdapter, mInsn);
      } else if ("be/coekaerts/wouter/flowtracker/test/FlowTester".equals(mInsn.owner)) {
        if ("createSourceChar".equals(mInsn.name) || "createSourceByte".equals(mInsn.name)) {
          return new TesterValue(flowMethodAdapter, mInsn);
        }
      } else if ("java/lang/Byte".equals(mInsn.owner) && "toUnsignedInt".equals(mInsn.name)) {
        return values.get(0);
      }
    }
    return toFlowValue(basicInterpreter.naryOperation(insn, values));
  }

  @Override
  public void returnOperation(AbstractInsnNode insn, FlowValue value, FlowValue expected) {
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

      // TODO we need to handle merging TrackableValue with MergedValue (WIP)
      //  (because it does multiple passes?). see also SourceInterpreter.
      // TODO (ideally we'd also do this if only one of the two is trackable)
      if ((value1 instanceof TrackableValue || value1 instanceof MergedValue)
          && (value2 instanceof TrackableValue || value2 instanceof MergedValue)) {
        return new MergedValue(mergeTypes(type1, type2), mergingFrame, value1, value2);
      }

      // this is duplicating logic in mergeTypes; to avoid creating unnecessary extra FlowValue
      // instances
      if (type1 == null || type2 == null) {
        return FlowValue.UNINITIALIZED_VALUE;
      } else if (type1.getSort() == Type.OBJECT && type2.getSort() == Type.OBJECT) {
        return FlowValue.REFERENCE_VALUE;
      } else {
        return FlowValue.UNINITIALIZED_VALUE;
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
