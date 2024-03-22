package be.coekaerts.wouter.flowtracker.weaver.flow;

import static be.coekaerts.wouter.flowtracker.weaver.flow.InvocationArgStore.shouldInstrumentInvocationArg;

import be.coekaerts.wouter.flowtracker.util.Logger;
import be.coekaerts.wouter.flowtracker.weaver.Transformer;
import be.coekaerts.wouter.flowtracker.weaver.Types;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class FlowAnalyzingTransformer implements Transformer {
  private static final Logger logger = new Logger("AsmTransformer");

  private final Commentator commentator;
  private final AnalysisListener listener;

  public FlowAnalyzingTransformer() {
    this.commentator = new Commentator(); // noop Commentator
    this.listener = new AnalysisListener(); // noop Listener
  }

  public FlowAnalyzingTransformer(Commentator commentator, AnalysisListener listener) {
    this.commentator = commentator;
    this.listener = listener;
  }

  public FlowAnalyzingTransformer(AnalysisListener listener) {
    this.commentator = new Commentator(); // noop Commentator
    this.listener = listener;
  }

  private class FlowClassAdapter extends ClassVisitor {
    private final String className;
    private final ConstantsTransformation constantsTransformation;

    private FlowClassAdapter(String className, ClassVisitor cv) {
      super(Opcodes.ASM9, cv);
      this.className = className;
      this.constantsTransformation = new ConstantsTransformation(className);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      if (!className.equals(name)) {
        throw new IllegalStateException("Class name mismatch: " + name + " != " + className);
      }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new FlowMethodAdapter(mv, className, access, name, desc, signature, exceptions,
          constantsTransformation);
    }
  }

  class FlowMethodAdapter extends MethodNode {
    final String owner;
    /** The next visitor in the chain after this one */
    private final TransparentLocalVariablesSorter varSorter;
    final InsnList intro = new InsnList();
    final InvocationIncomingTransformation invocation = new InvocationIncomingTransformation();
    final ConstantsTransformation constantsTransformation;

    private FlowMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc,
        String signature, String[] exceptions, ConstantsTransformation constantsTransformation) {
      super(Opcodes.ASM9, access, name, desc, signature, exceptions);
      this.owner = owner;
      this.varSorter = new TransparentLocalVariablesSorter(access, desc, mv);
      this.constantsTransformation = constantsTransformation;
    }

    @Override
    public void visitEnd() {
      try {
        doVisitEnd();
      } catch (Exception e) {
        throw new RuntimeException("Exception handling " + owner + " " + name + " " + desc, e);
      }
    }

    private void doVisitEnd() {
      super.visitEnd();
      FlowInterpreter interpreter = new FlowInterpreter(this);
      FlowAnalyzer analyzer = new FlowAnalyzer(interpreter, this);
      Frame<FlowValue>[] frames;

      try {
        frames = analyzer.analyze(owner, this);
      } catch (Exception e) {
        // up to this point we haven't made any changes yet, so we can handle failures somewhat
        // gracefully by just outputting what we have now. that way at least the other methods in
        // the same class can still get transformed.
        // tip: to debug, using dumpAsm() here can be useful. (but not outputting that automatically
        // because it's very verbose).
        logger.error(e, "Exception analyzing " + owner + " " + name + " " + desc);
        listener.error(e);
        this.accept(TransparentLocalVariablesSorter.bypass(varSorter));
        return;
      }

      List<Store> stores = new ArrayList<>();

      for (int i = 0; i < instructions.size(); i++) {
        AbstractInsnNode insn = instructions.get(i);
        FlowFrame frame = (FlowFrame) frames[i];
        if (insn.getOpcode() == Opcodes.CASTORE) {
          stores.add(ArrayStore.createCharArrayStore((InsnNode) insn, frame));
        } else if (insn.getOpcode() == Opcodes.BASTORE
            && Types.BYTE_ARRAY.equals(frame.getStack(frame.getStackSize() - 3).getType())) {
          stores.add(ArrayStore.createByteArrayStore((InsnNode) insn, frame));
        } else if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
            || insn.getOpcode() == Opcodes.INVOKESTATIC
            || insn.getOpcode() == Opcodes.INVOKESPECIAL) {
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
          } else if ("clone".equals(mInsn.name)
              && (mInsn.owner.equals("[C") || mInsn.owner.equals("[B"))) {
            mInsn.desc = mInsn.owner.equals("[C") ? "([C)[C" : "([B)[B";
            mInsn.owner = "be/coekaerts/wouter/flowtracker/hook/ArrayHook";
            mInsn.setOpcode(Opcodes.INVOKESTATIC);
          } else if (mInsn.owner.equals("be/coekaerts/wouter/flowtracker/test/FlowTester")) {
            if (mInsn.name.equals("assertTrackedValue")) {
              stores.add(new TesterStore(mInsn, frame, 3));
            } else if (mInsn.name.equals("assertIsTheTrackedValue")
                || mInsn.name.equals("getCharSourceTracker")
                || mInsn.name.equals("getCharSourcePoint")
                || mInsn.name.equals("getByteSourceTracker")
                || mInsn.name.equals("getByteSourcePoint")) {
              stores.add(new TesterStore(mInsn, frame, 0));
            }
          } else if (shouldInstrumentInvocationArg(mInsn.name, mInsn.desc)) {
            stores.add(new InvocationArgStore(mInsn, frame,
                // next frame, might contain the return value of the call
                i + 1 < frames.length ? (FlowFrame) frames[i+1] : null));
          }
        } else if (insn.getOpcode() == Opcodes.IRETURN
            && InvocationReturnValue.shouldInstrumentInvocation(name, desc)) {
          stores.add(new InvocationReturnStore((InsnNode) insn, frame, invocation));
        } else if (insn.getOpcode() == Opcodes.PUTFIELD) {
          stores.add(new FieldStore((FieldInsnNode) insn, frame));
        }
      }

      listener.analysed(this, frames, stores);

      for (Store store : stores) {
        store.insertTrackStatements(this);
      }

      this.instructions.insert(intro);

      // send the result to the next MethodVisitor
      this.accept(varSorter);
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

    /**
     * Dump ASM code to generate the currently analyzed method to stderr.
     * This can be useful when debugging a problem with "real" code, to help extract a minimal test
     * case.
     */
    @SuppressWarnings("unused") // can be called from a debugger
    void dumpAsm() {
      ASMifier asmifier = new ASMifier();
      // only care about "owner" field, because that's all that FlowClassAdapter.visit cares about
      asmifier.visit(0, 0, owner, null, null, new String[0]);
      ASMifier methodASMifier = asmifier.visitMethod(access, name, desc, signature,
          exceptions.toArray(String[]::new));
      this.accept(new TraceMethodVisitor(methodASMifier));
      StringWriter sw = new StringWriter();
      asmifier.print(new PrintWriter(sw));
      System.err.println(sw);
    }
  }

  public ClassVisitor transform(String className, ClassVisitor cv) {
    return new FlowClassAdapter(className, cv);
  }

  public static class AnalysisListener {
    void analysed(FlowMethodAdapter flowMethodAdapter, Frame<FlowValue>[] frames,
        List<Store> stores) {
    }

    void error(Throwable t) {
    }
  }
}
