package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.ClassAdapterFactory;
import be.coekaerts.wouter.flowtracker.weaver.Types;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class FlowAnalyzingTransformer implements ClassAdapterFactory {
  private final Commentator commentator;

  public FlowAnalyzingTransformer() {
    this.commentator = new Commentator(); // noop Commentator
  }

  FlowAnalyzingTransformer(Commentator commentator) {
    this.commentator = commentator;
  }

  private class FlowClassAdapter extends ClassVisitor {
    private String name;

    public FlowClassAdapter(ClassVisitor cv) {
      super(Opcodes.ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      this.name = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new FlowMethodAdapter(mv, this.name, access, name, desc, signature, exceptions);
    }
  }

  class FlowMethodAdapter extends MethodNode {
    private final String owner;
    /** The next visitor in the chain after this one */
    private final TransparentLocalVariablesSorter varSorter;
    final InsnList intro = new InsnList();
    private final InvocationTransformation invocation = new InvocationTransformation();

    public FlowMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc,
        String signature, String[] exceptions) {
      super(Opcodes.ASM9, access, name, desc, signature, exceptions);
      this.owner = owner;
      this.varSorter = new TransparentLocalVariablesSorter(access, desc, mv);
    }

    @Override
    public void visitEnd() {
      try {
        doVisitEnd();
      } catch (Exception e) {
        throw new RuntimeException("Exception handling " + owner + " " + name + " " + desc, e);
      }
    }

    private void doVisitEnd() throws AnalyzerException {
      super.visitEnd();
      FlowInterpreter interpreter = new FlowInterpreter(this);
      Analyzer<BasicValue> analyzer = new Analyzer<>(interpreter);
      Frame<BasicValue>[] frames = analyzer.analyze(owner, this);

      List<Store> stores = new ArrayList<>();

      for (int i = 0; i < instructions.size(); i++) {
        AbstractInsnNode insn = instructions.get(i);
        Frame<BasicValue> frame = frames[i];
        if (insn.getOpcode() == Opcodes.CASTORE) {
          stores.add(ArrayStore.createCharArrayStore((InsnNode) insn, frame));
        } else if (insn.getOpcode() == Opcodes.BASTORE
            && Types.BYTE_ARRAY.equals(frame.getStack(frame.getStackSize() - 3).getType())) {
          stores.add(ArrayStore.createByteArrayStore((InsnNode) insn, frame));
        } else if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
            || insn.getOpcode() == Opcodes.INVOKESTATIC) {
          MethodInsnNode mInsn = (MethodInsnNode) insn;

          if ("java/lang/System".equals(mInsn.owner) && "arraycopy".equals(mInsn.name)
              && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(mInsn.desc)) {
            // if it is a copy from char[] to char[] or from byte[] to byte[]
            Type sourceType = frame.getStack(frame.getStackSize() - 5).getType();
            Type destType = frame.getStack(frame.getStackSize() - 3).getType();
            if ((Types.CHAR_ARRAY.equals(sourceType) && Types.CHAR_ARRAY.equals(destType))
                || (Types.BYTE_ARRAY.equals(sourceType) && Types.BYTE_ARRAY.equals(destType))) {
              // replace it with a call to our hook instead
              mInsn.owner = "be/coekaerts/wouter/flowtracker/hook/SystemHook";
            }
          } else if ("append".equals(mInsn.name)
              && ("java/lang/StringBuilder".equals(mInsn.owner)
              || "java/lang/StringBuffer".equals(mInsn.owner))
              && mInsn.desc.startsWith("(C)")) {
            stores.add(new AppendStore(mInsn, frame));
          } else if (mInsn.owner.equals("be/coekaerts/wouter/flowtracker/test/FlowTester")) {
            if (mInsn.name.equals("assertTrackedValue")) {
              stores.add(new TesterStore(mInsn, frame, 2));
            } else if (mInsn.name.equals("assertIsTheTrackedValue")
                || mInsn.name.equals("getCharSourceTracker")
                || mInsn.name.equals("getCharSourceIndex")
                || mInsn.name.equals("getByteSourceTracker")
                || mInsn.name.equals("getByteSourceIndex")) {
              stores.add(new TesterStore(mInsn, frame, 0));
            }
          }
        } else if (insn.getOpcode() == Opcodes.IRETURN
            && InvocationReturnValue.shouldInstrumentInvocation(name, desc)) {
          stores.add(new InvocationReturnStore((InsnNode) insn, frame, invocation));
        }
      }

      for (Store store : stores) {
        store.insertTrackStatements(this);
      }

      this.instructions.insert(intro);

      // send the result to the next MethodVisitor
      this.accept(varSorter);
    }

    /**
     * Create a new local variable for storing the index of a tracker, that is an int initialized
     * to -1
     */
    TrackLocal newLocalForIndex(String sourceForComment) {
      return newLocal(Type.INT_TYPE, List.of(new LdcInsnNode(-1)), sourceForComment);
    }

    /** Create a new local variable for storing an object, initialized to null */
    TrackLocal newLocalForObject(Type type, String sourceForComment) {
      return newLocal(type, List.of(new InsnNode(Opcodes.ACONST_NULL)), sourceForComment);
    }

    /**
     * Create a new local variable that can be used by our added code.
     * <p>
     * We get an index for these variables at the beginning of the method (from the
     * {@link #varSorter}), and ensure they are initialized. That way they can be accessed by any of
     * our added code without worrying about where they have definitely been set. Also, adding new
     * variables anywhere else with {@link LocalVariablesSorter} in a method that has frames (jumps)
     * is practically impossible because it does not properly update frames (see
     * <a href="https://gitlab.ow2.org/asm/asm/-/issues/316352">asm#316352</a>).
     */
    TrackLocal newLocal(Type type, List<AbstractInsnNode> initialValue, String sourceForComment) {
      TrackLocal local = new TrackLocal(type, varSorter.newLocal(type));
      // initialize the variable to -1 at the start of the method
      // NICE only initialize when necessary (if there is a jump, or it is read before it is first
      //  written to)
      addComment(intro, "Initialize newLocal %s", sourceForComment);
      for (AbstractInsnNode initialValueInstruction : initialValue) {
        intro.add(initialValueInstruction);
      }
      intro.add(local.store());
      return local;
    }

    void addComment(InsnList insnList, String comment, Object... commentArgs) {
      commentator.addComment(insnList, comment, commentArgs);
    }
  }

  private static class FlowInterpreter extends BasicInterpreter {
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

  public ClassVisitor createClassAdapter(ClassVisitor cv) {
    return new FlowClassAdapter(cv);
  }
}
