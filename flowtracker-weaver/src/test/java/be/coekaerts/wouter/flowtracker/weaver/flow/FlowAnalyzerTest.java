package be.coekaerts.wouter.flowtracker.weaver.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.AnalysisListener;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
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
  public void testCharAtValue() {
    getReturnValue(CharAtValue.class, new Object() {
      @SuppressWarnings("unused")
      char go(String s) {
        return s.charAt(0);
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
    assertTrue(v.getOriginal() instanceof InvocationArgValue);
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
    assertEquals(v.values.size(), 2);
  }

  static <T extends FlowValue> T getReturnValue(Class<T> clazz, Object o) {
    return clazz.cast(getReturnValue(o));
  }

  static FlowValue getReturnValue(Object o) {
    return Stream.of(getFrames(o))
        .map(f -> (FlowFrame) f)
        .filter(f -> f.getInsn().getOpcode() == Opcodes.IRETURN)
        .findFirst()
        .map(f -> f.getStack(0))
        .orElseThrow();
  }

  static Frame<FlowValue>[] getFrames(Object o) {
    String className = o.getClass().getName();
    TestAnalysisListener listener = new TestAnalysisListener();

    ClassVisitor transformer =
        new FlowAnalyzingTransformer(new Commentator(), listener)
            .createClassAdapter(new ClassWriter(0));

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
    List<Store> stores;

    @Override
    void analysed(FlowMethodAdapter flowMethodAdapter, Frame<FlowValue>[] frames,
        List<Store> stores) {
      if (flowMethodAdapter.name.equals("<init>")) {
        return;
      }
      this.frames = frames;
      this.stores = stores;
    }
  }
}
