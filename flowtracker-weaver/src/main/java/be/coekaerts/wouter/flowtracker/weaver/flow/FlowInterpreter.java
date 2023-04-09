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

class FlowInterpreter extends BasicInterpreter {

  private final FlowMethodAdapter flowMethodAdapter;

  public FlowInterpreter(FlowMethodAdapter flowMethodAdapter) {
    super(Opcodes.ASM9);
    this.flowMethodAdapter = flowMethodAdapter;
  }

  @Override
  public BasicValue newValue(Type type) {
    // for char[] and byte[] remember the exact type
    if (Types.CHAR_ARRAY.equals(type) || Types.BYTE_ARRAY.equals(type)) {
      return new BasicValue(type);
    }
    // for others the exact type doesn't matter
    return super.newValue(type);
  }

  @Override
  public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
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
        return new ConstantValue(flowMethodAdapter, Type.INT_TYPE, flowMethodAdapter.owner);
    }
    return super.newOperation(insn);
  }

  @Override
  public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value)
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

    return super.unaryOperation(insn, value);
  }

  @Override
  public BasicValue binaryOperation(AbstractInsnNode aInsn, BasicValue value1, BasicValue value2)
      throws AnalyzerException {
    switch (aInsn.getOpcode()) {
      case CALOAD: {
        InsnNode insn = (InsnNode) aInsn;
        return new ArrayLoadValue(flowMethodAdapter, insn, Type.CHAR_TYPE, Types.CHAR_ARRAY);
      }
      case BALOAD: {
        InsnNode insn = (InsnNode) aInsn;
        return new ArrayLoadValue(flowMethodAdapter, insn, Type.BYTE_TYPE, Types.BYTE_ARRAY);
      }
      case IAND: {
        // treat `x & constant` as having the same source as x
        if (value2 instanceof ConstantValue) {
          return value1;
        } else if (value1 instanceof ConstantValue) {
          return value2;
        }
      }
    }
    return super.binaryOperation(aInsn, value1, value2);
  }

  @Override
  public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values)
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
    return super.naryOperation(insn, values);
  }
}
