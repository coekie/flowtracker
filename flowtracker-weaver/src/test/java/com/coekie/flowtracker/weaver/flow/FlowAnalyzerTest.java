package com.coekie.flowtracker.weaver.flow;

import static com.google.common.truth.Truth.assertThat;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.RET;
import static org.objectweb.asm.Opcodes.RETURN;

import com.coekie.flowtracker.util.Config;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.AnalysisListener;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Frame;


public class FlowAnalyzerTest {
  @Test
  public void testConstantValue() {
    getReturnValue(ConstantValue.class, new Object() {
      @SuppressWarnings("unused")
      char go() {
        return 'c';
      }
    });
  }

  @Test
  public void testArrayLoadValue() {
    getReturnValue(ArrayLoadValue.class, new Object() {
      @SuppressWarnings("unused")
      char go(char[] in) {
        return in[0];
      }
    });
  }

  @Test
  public void testInvocationArgValue() {
    CopyValue v = getReturnValue(CopyValue.class, new Object() {
      @SuppressWarnings("unused")
      char go(char c) {
        return c;
      }
    });
    assertThat(v.getOriginal()).isInstanceOf(InvocationArgValue.class);
  }

  @Test
  public void testFieldValue() {
    getReturnValue(FieldValue.class, new Object() {
      char c;
      @SuppressWarnings("unused")
      char go() {
        return c;
      }
    });
  }

  @Test
  public void testMergedValue() {
    MergedValue v = getReturnValue(MergedValue.class, new Object() {
      char c1;
      char c2;

      @SuppressWarnings("unused")
      char go(boolean b) {
        return b ? c1 : c2;
      }
    });
    assertThat(v.values.size()).isEqualTo(2);
  }

  // this is a bit of a weird test case. this is testing if when two long values get merged, that
  // we still retain the fact that they are longs (if not, we get an AnalyzerException
  // "Cannot pop operand off an empty stack").
  // This very rarely matters, but got triggered by "oldSum == (oldSum = checkSum)" in
  // ForkJoinPool.tryTerminate.
  @Test public void testMergeLongValues() {
    FlowValue v = getReturnValue(FlowValue.class, new Object() {
      @SuppressWarnings("all")
      long go(boolean bool, long a, long b) {
        if (bool) {
          b = 1;
        }
        return (a = b);
      }
    });
    assertThat(v.getType().getSort()).isEqualTo(Type.LONG);
  }

  // template for a test case that can be used with output of FlowMethod.dumpAsm, to test
  // analysis/transformation of extracted bytecode.
  @Test public void testAsm() {
    TestAnalysisListener listener = new TestAnalysisListener();
    ClassVisitor classVisitor = new FlowTransformer(Config.empty(), listener).transform(
        "java/Example", new ClassWriter(0));
    classVisitor.visit(0, 0, "java/Example", null, null, null);
    {
      MethodVisitor methodVisitor =
          classVisitor.visitMethod(ACC_PRIVATE, "example", "()Z", null, null);
      methodVisitor.visitCode();
      methodVisitor.visitInsn(ICONST_1);
      methodVisitor.visitInsn(IRETURN);
      methodVisitor.visitMaxs(1, 1);
      methodVisitor.visitEnd();
    }
    listener.checkSuccess();
  }

  // basic (not really produced by a compiler) example using old JSR instruction.
  // JSR is only used by code compiled for older java versions.
  @Test public void testJsr() {
    TestAnalysisListener listener = new TestAnalysisListener();
    ClassVisitor classVisitor = new FlowTransformer(Config.empty(), listener).transform(
        "java/Example", new ClassWriter(0));
    classVisitor.visit(0, 0, "java/Example", null, null, null);
    {
      MethodVisitor methodVisitor =
          classVisitor.visitMethod(ACC_PRIVATE, "example", "()V", null, null);
      methodVisitor.visitCode();

      Label label = new Label();
      methodVisitor.visitJumpInsn(JSR, label);
      methodVisitor.visitInsn(RETURN);

      methodVisitor.visitLabel(label);
      methodVisitor.visitVarInsn(ASTORE, 1);
      methodVisitor.visitVarInsn(RET, 1);

      methodVisitor.visitMaxs(2, 2);
      methodVisitor.visitEnd();
    }

    listener.checkSuccess();
  }

  static <T extends FlowValue> T getReturnValue(Class<T> clazz, Object o) {
    return clazz.cast(getReturnValue(o));
  }

  static FlowValue getReturnValue(Object o) {
    return Stream.of(getFrames(o))
        .map(f -> (FlowFrame) f)
        .filter(f -> f.getInsn().getOpcode() >= Opcodes.IRETURN
            && f.getInsn().getOpcode() <= Opcodes.ARETURN)
        .findFirst()
        .map(f -> f.getStack(0))
        .orElseThrow();
  }

  static Frame<FlowValue>[] getFrames(Object o) {
    String className = o.getClass().getName();
    TestAnalysisListener listener = new TestAnalysisListener();

    ClassVisitor transformer =
        new FlowTransformer(Config.empty(), listener)
            .transform(Type.getInternalName(o.getClass()), new ClassWriter(0));

    try {
      new ClassReader(className)
          .accept(transformer, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return listener.frames;
  }

  static class TestAnalysisListener extends AnalysisListener {
    Frame<FlowValue>[] frames;
    List<Instrumentable> toInstrument;
    Throwable error;

    @Override
    void analysed(FlowMethod method, Frame<FlowValue>[] frames,
        List<Instrumentable> toInstrument) {
      if (method.name.equals("<init>")) {
        return;
      }
      this.frames = frames;
      this.toInstrument = toInstrument;
    }

    @Override
    void error(Throwable t) {
      error = t;
    }

    void checkSuccess() {
      if (error != null) {
        throw new AssertionError("Analysis failed", error);
      }
      if (frames == null || toInstrument == null) {
        throw new AssertionError("Analysis did not succeed");
      }
    }
  }
}
