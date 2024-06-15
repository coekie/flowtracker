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

import com.coekie.flowtracker.util.Config;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.weaver.ClassFilter;
import com.coekie.flowtracker.weaver.Transformer;
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
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceMethodVisitor;

/** Our main bytecode {@link Transformer}, that runs dataflow analysis on code (using
 * {@link FlowAnalyzer} and {@link FlowInterpreter}), finds relevant places to instrument
 * ({@link Instrumentable}s), and instruments them.
 */
public class FlowTransformer implements Transformer {
  private static final Logger logger = new Logger("FlowTransformer");

  private final Commentator commentator;
  private final AnalysisListener listener;
  private final ClassFilter breakStringInterningFilter;

  public FlowTransformer(Config config) {
    this(config,
        new Commentator(), // noop Commentator
        new AnalysisListener()); // noop Listener

  }

  public FlowTransformer(Config config, Commentator commentator) {
    this(config, commentator,
        new AnalysisListener()); // noop Listener
  }

  public FlowTransformer(Config config, AnalysisListener listener) {
    this(config,
        new Commentator(), // noop Commentator
        listener);
  }

  private FlowTransformer(Config config, Commentator commentator,
      AnalysisListener listener) {
    this.commentator = commentator;
    this.listener = listener;
    this.breakStringInterningFilter = new ClassFilter(
        config.get("breakStringInterning", "%recommended,+*"),
        // by default, we don't break String interning in most JDK classes, because some of them
        // depend on interning to work. we don't do that for other libraries, so this probably
        // breaks some libraries.
        "+java.net.*,+java.io.*,-java.*,+sun.net.*,-sun.*,+jdk.internal.net.*,-jdk.*");
  }

  private class FlowClassAdapter extends ClassVisitor {
    private final String className;
    private final ConstantsTransformation constantsTransformation;
    private int version;

    private FlowClassAdapter(ClassLoader classLoader, String className, ClassVisitor cv) {
      super(Opcodes.ASM9, cv);
      this.className = className;
      this.constantsTransformation =
          new ConstantsTransformation(classLoader, className, breakStringInterningFilter);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      this.version = version;
      if (!className.equals(name)) {
        throw new IllegalStateException("Class name mismatch: " + name + " != " + className);
      }
    }

    @Override
    public void visitSource(String source, String debug) {
      super.visitSource(source, debug);
      constantsTransformation.initSourceFile(source);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new FlowMethod(mv, className, version, access, name, desc, signature,
          exceptions, constantsTransformation);
    }
  }

  class FlowMethod extends MethodNode {
    final String owner;
    final int version;
    /** The next visitor in the chain after this one */
    private final TransparentLocalVariablesSorter varSorter;
    final InsnList intro = new InsnList();
    final ContextLoader contextLoader = new ContextLoader();
    final InvocationIncomingTransformation invocation = new InvocationIncomingTransformation();
    final ConstantsTransformation constantsTransformation;
    int[] lineNumbers;

    private FlowMethod(MethodVisitor mv, String owner, int version, int access, String name,
        String desc, String signature, String[] exceptions,
        ConstantsTransformation constantsTransformation) {
      super(Opcodes.ASM9, access, name, desc, signature, exceptions);
      this.owner = owner;
      this.version = version;
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

    private void initLineNumbers() {
      if (lineNumbers == null) {
        lineNumbers = new int[instructions.size()];
        int line = -1;
        for (int i = 0; i < instructions.size(); i++) {
          AbstractInsnNode insn = instructions.get(i);
          if (insn instanceof LineNumberNode) {
            LineNumberNode lnn = (LineNumberNode) insn;
            line = lnn.line;
          }
          lineNumbers[i] = line;
        }
      }
    }

    int getLine(AbstractInsnNode insn) {
      initLineNumbers();
      if (instructions.size() != lineNumbers.length) {
        // getLine should be called before we start instrumenting
        throw new IllegalStateException("Cannot get line numbers after instructions were modified");
      }
      return lineNumbers[instructions.indexOf(insn)];
    }

    private void doVisitEnd() {
      super.visitEnd();
      FlowInterpreter interpreter = new FlowInterpreter(this);
      FlowAnalyzer analyzer = new FlowAnalyzer(interpreter, this);
      Frame<FlowValue>[] frames;
      List<Instrumentable> toInstrument = new ArrayList<>();

      try {
        frames = analyzer.analyze(owner, this);
        for (int i = 0; i < instructions.size(); i++) {
          analyzeInstruction(toInstrument, i, frames);
        }
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

      listener.analysed(this, frames, toInstrument);

      for (Instrumentable instrumentable : toInstrument) {
        instrumentable.instrument(this);
      }

      this.instructions.insert(intro);

      // send the result to the next MethodVisitor
      this.accept(varSorter);
    }

    /** Analyze a single instruction, adding to `toInstrument` when we need to instrument it */
    private void analyzeInstruction(List<Instrumentable> toInstrument, int insnIndex,
        Frame<FlowValue>[] frames) {
      AbstractInsnNode insn = instructions.get(insnIndex);
      FlowFrame frame = (FlowFrame) frames[insnIndex];
      switch (insn.getOpcode()) {
        case Opcodes.CASTORE:
          ArrayStore.analyzeCharArrayStore(toInstrument, (InsnNode) insn, frame);
          break;
        case Opcodes.BASTORE:
          ArrayStore.analyzeByteArrayStore(toInstrument, (InsnNode) insn, frame);
          break;
        case Opcodes.IASTORE:
          ArrayStore.analyzeIntArrayStore(toInstrument, (InsnNode) insn, frame, owner);
          break;
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKEINTERFACE:
          MethodInsnNode mInsn = (MethodInsnNode) insn;
          boolean instrumented =
              ArrayCopyCall.analyze(toInstrument, mInsn, frame)
                  || ArrayCloneCall.analyze(toInstrument, mInsn)
                  || ClassNameCall.analyze(toInstrument, mInsn)
                  || FieldNameCall.analyze(toInstrument, mInsn)
                  || MethodNameCall.analyze(toInstrument, mInsn)
                  || TesterStore.analyze(toInstrument, mInsn, frame)
                  || UnsafeStore.analyze(toInstrument, mInsn, frame);
          if (!instrumented) { // don't instrument twice for invocations already handled above
            InvocationArgStore.analyze(toInstrument, mInsn, frame, frames, insnIndex);
          }
          break;
        case Opcodes.INVOKEDYNAMIC:
          StringConcatenation.analyze(toInstrument, (InvokeDynamicInsnNode) insn, frame);
          break;
        case Opcodes.IRETURN:
          InvocationReturnStore.analyze(toInstrument, (InsnNode) insn, frame, this);
          break;
        case Opcodes.PUTFIELD:
          FieldStore.analyze(toInstrument, (FieldInsnNode) insn, frame);
          break;
        case Opcodes.LDC:
          StringLdc.analyze(toInstrument, (LdcInsnNode) insn, frame);
          break;
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
          StringComparison.analyze(toInstrument, (JumpInsnNode) insn, frame, owner);
          break;
      }
    }

    /** Create a new local variable for storing an object, initialized to null */
    TrackLocal newLocalForObject(Type type, String sourceForComment) {
      return newLocal(type, List.of(new InsnNode(Opcodes.ACONST_NULL)), 1, sourceForComment);
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
    TrackLocal newLocal(Type type, List<AbstractInsnNode> initialValue, int maxStack,
        String sourceForComment) {
      TrackLocal local = new TrackLocal(type, varSorter.newLocal(type));
      // initialize the variable to -1 at the start of the method
      // NICE only initialize when necessary (if there is a jump, or it is read before it is first
      //  written to)
      addComment(intro, "Initialize newLocal %s", sourceForComment);
      for (AbstractInsnNode initialValueInstruction : initialValue) {
        intro.add(initialValueInstruction);
      }
      intro.add(local.store());
      this.maxStack = Math.max(this.maxStack, maxStack);
      return local;
    }

    /** @see Commentator */
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

    boolean canUseConstantDynamic() {
      return version >= Opcodes.V11
          // avoid infinite recursion by trying to use condy in condy implementation
          && !owner.startsWith("java/lang/invoke")
          && !owner.startsWith("java/lang/Class")
          && !owner.startsWith("java/lang/String")
          && !owner.equals("java/lang/Integer")
          && !owner.startsWith("sun/invoke")
          && !owner.startsWith("jdk/internal/org/objectweb/asm")
          // for testing
          && !owner.equals("com/coekie/flowtracker/test/StringTest$NoCondy");
    }
  }

  @Override
  public ClassVisitor transform(ClassLoader classLoader, String className, ClassVisitor cv) {
    return new FlowClassAdapter(classLoader, className, cv);
  }

  public static class AnalysisListener {
    void analysed(FlowMethod method, Frame<FlowValue>[] frames,
        List<Instrumentable> toInstrument) {
    }

    void error(Throwable t) {
    }
  }
}
