package be.coekaerts.wouter.flowtracker.weaver.flow;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * Tests for FlowAnalyzingTransformer by inspecting generated bytecode. flowtracker mostly tests
 * FlowAnalyzingTransformer indirectly, by executing instrumented code, but this test class is an
 * exception to that: testing if the bytecode looks as expected.
 *
 * <p>These tests are fragile (depend on the exact bytecode generated by the compiler), and are
 * completely written by copy-pasting the actual output as the expected output, and they don't check
 * that the generated code actually does what we want it to do. It only covers a couple
 * transformations, because doing this for everything would be too verbose. So in that sense this is
 * not a great test class at all. But seeing the original and generated bytecode next to each other
 * makes this a convenient tool to help understand what's going on with the bytecode exactly, and
 * for debugging.
 */
public class FlowAnalyzingTransformerTest {

  @Test
  public void testArraycopy() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    void t(byte[] bytes1, byte[] bytes2) {
                      System.arraycopy(bytes1, 1, bytes2, 2, 3);
                    }
                  },
        // original code
        "ALOAD 1\n"
            + "ICONST_1\n"
            + "ALOAD 2\n"
            + "ICONST_2\n"
            + "ICONST_3\n"
            + "INVOKESTATIC java/lang/System.arraycopy (Ljava/lang/Object;ILjava/lang/Object;II)V\n"
            + "RETURN\n"
            + "MAXSTACK = 5\n"
            + "MAXLOCALS = 3\n",
        // transformed code
        "ALOAD 1\n"
            + "ICONST_1\n"
            + "ALOAD 2\n"
            + "ICONST_2\n"
            + "ICONST_3\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/SystemHook.arraycopy (Ljava/lang/Object;ILjava/lang/Object;II)V\n"
            + "RETURN\n"
            + "MAXSTACK = 5\n"
            + "MAXLOCALS = 3\n");
  }

  /**
   * Test insertion of code to get tracker and pass it to a hook, using the simple example of an
   * array load and store
   */
  @Test
  public void testTracker() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    void t(byte[] bytes1, byte[] bytes2) {
                      bytes1[1] = bytes2[2];
                    }
                  },
        // original code
        "ALOAD 1\n"
            + "ICONST_1\n"
            + "ALOAD 2\n"
            + "ICONST_2\n"
            + "BALOAD\n"
            + "BASTORE\n"
            + "RETURN\n"
            + "MAXSTACK = 4\n"
            + "MAXLOCALS = 3\n",
        // transformed code
        "// Initialize newLocal ArrayLoadValue array\n"
            + "ACONST_NULL\n"
            + "ASTORE 3\n"
            + "// Initialize newLocal ArrayLoadValue index\n"
            + "LDC -1\n"
            + "ISTORE 4\n"
            + "// Initialize newLocal ArrayLoadValue PointTracker\n"
            + "ACONST_NULL\n"
            + "ASTORE 5\n"
            + "ALOAD 1\n"
            + "ICONST_1\n"
            + "ALOAD 2\n"
            + "ICONST_2\n"
            + "// begin ArrayLoadValue.insertTrackStatements\n"
            + "ISTORE 4\n"
            + "ASTORE 3\n"
            + "ALOAD 3\n"
            + "ILOAD 4\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayLoadHook.getElementTracker (Ljava/lang/Object;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;\n"
            + "ASTORE 5\n"
            + "ALOAD 3\n"
            + "ILOAD 4\n"
            + "// end ArrayLoadValue.insertTrackStatements\n"
            + "BALOAD\n"
            + "// begin ArrayStore.insertTrackStatements: ArrayHook.set*(array, arrayIndex, value [already on stack], source, sourceIndex)\n"
            + "// ArrayLoadValue.loadSourceTracker: PointTracker.getTracker(pointTracker)\n"
            + "ALOAD 5\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getTracker (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;\n"
            + "// ArrayLoadValue.loadSourceIndex: PointTracker.getIndex(pointTracker)\n"
            + "ALOAD 5\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getIndex (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)I\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayHook.setByte ([BIBLbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V\n"
            + "// end ArrayStore.insertTrackStatements\n"
            + "RETURN\n"
            + "MAXSTACK = 6\n"
            + "MAXLOCALS = 6\n");
  }

  @Test
  public void testFrames() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    void t(char[] out, char[] in, boolean b) {
                      char ch = in[0];
                      if (b) {
                        return;
                      }
                      out[0] = ch;
                    }
                  },
        // original code
        "ALOAD 2\n"
            + "ICONST_0\n"
            + "CALOAD\n"
            + "ISTORE 4\n"
            + "ILOAD 3\n"
            + "IFEQ L0\n"
            + "RETURN\n"
            + "L0\n"
            + "FRAME FULL [$THIS$ [C [C I I] []\n"
            + "ALOAD 1\n"
            + "ICONST_0\n"
            + "ILOAD 4\n"
            + "CASTORE\n"
            + "RETURN\n"
            + "MAXSTACK = 3\n"
            + "MAXLOCALS = 5\n",
        // transformed code
        "// Initialize newLocal ArrayLoadValue array\n"
            + "ACONST_NULL\n"
            + "ASTORE 4\n"
            + "// Initialize newLocal ArrayLoadValue index\n"
            + "LDC -1\n"
            + "ISTORE 5\n"
            + "// Initialize newLocal ArrayLoadValue PointTracker\n"
            + "ACONST_NULL\n"
            + "ASTORE 6\n"
            + "ALOAD 2\n"
            + "ICONST_0\n"
            + "// begin ArrayLoadValue.insertTrackStatements\n"
            + "ISTORE 5\n"
            + "ASTORE 4\n"
            + "ALOAD 4\n"
            + "ILOAD 5\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayLoadHook.getElementTracker (Ljava/lang/Object;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;\n"
            + "ASTORE 6\n"
            + "ALOAD 4\n"
            + "ILOAD 5\n"
            + "// end ArrayLoadValue.insertTrackStatements\n"
            + "CALOAD\n"
            + "ISTORE 7\n"
            + "ILOAD 3\n"
            + "IFEQ L0\n"
            + "RETURN\n"
            + "L0\n"
            + "FRAME FULL [$THIS$ [C [C I [C I be/coekaerts/wouter/flowtracker/tracker/TrackerPoint I] []\n"
            + "ALOAD 1\n"
            + "ICONST_0\n"
            + "ILOAD 7\n"
            + "// begin ArrayStore.insertTrackStatements: ArrayHook.set*(array, arrayIndex, value [already on stack], source, sourceIndex)\n"
            + "// ArrayLoadValue.loadSourceTracker: PointTracker.getTracker(pointTracker)\n"
            + "ALOAD 6\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getTracker (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;\n"
            + "// ArrayLoadValue.loadSourceIndex: PointTracker.getIndex(pointTracker)\n"
            + "ALOAD 6\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getIndex (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)I\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayHook.setChar ([CICLbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V\n"
            + "// end ArrayStore.insertTrackStatements\n"
            + "RETURN\n"
            + "MAXSTACK = 6\n"
            + "MAXLOCALS = 8\n");
  }

  static char[] myCharArray;
  static boolean myBoolean;
  static InputStream inputStream;

  @Test
  public void testFramesLoop() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    void t() {
                      while (myBoolean) {
                        myCharArray[0] = myCharArray[1];
                      }
                    }
                  },
        // original code
        "L0\n"
            + "FRAME FULL [$THIS$] []\n"
            + "GETSTATIC $THISTEST$.myBoolean : Z\n"
            + "IFEQ L1\n"
            + "GETSTATIC $THISTEST$.myCharArray : [C\n"
            + "ICONST_0\n"
            + "GETSTATIC $THISTEST$.myCharArray : [C\n"
            + "ICONST_1\n"
            + "CALOAD\n"
            + "CASTORE\n"
            + "GOTO L0\n"
            + "L1\n"
            + "FRAME FULL [$THIS$] []\n"
            + "RETURN\n"
            + "MAXSTACK = 4\n"
            + "MAXLOCALS = 1\n",
        // transformed code
        "// Initialize newLocal ArrayLoadValue array\n"
            + "ACONST_NULL\n"
            + "ASTORE 1\n"
            + "// Initialize newLocal ArrayLoadValue index\n"
            + "LDC -1\n"
            + "ISTORE 2\n"
            + "// Initialize newLocal ArrayLoadValue PointTracker\n"
            + "ACONST_NULL\n"
            + "ASTORE 3\n"
            + "L0\n"
            + "FRAME FULL [$THIS$ [C I be/coekaerts/wouter/flowtracker/tracker/TrackerPoint] []\n"
            + "GETSTATIC $THISTEST$.myBoolean : Z\n"
            + "IFEQ L1\n"
            + "GETSTATIC $THISTEST$.myCharArray : [C\n"
            + "ICONST_0\n"
            + "GETSTATIC $THISTEST$.myCharArray : [C\n"
            + "ICONST_1\n"
            + "// begin ArrayLoadValue.insertTrackStatements\n"
            + "ISTORE 2\n"
            + "ASTORE 1\n"
            + "ALOAD 1\n"
            + "ILOAD 2\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayLoadHook.getElementTracker (Ljava/lang/Object;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;\n"
            + "ASTORE 3\n"
            + "ALOAD 1\n"
            + "ILOAD 2\n"
            + "// end ArrayLoadValue.insertTrackStatements\n"
            + "CALOAD\n"
            + "// begin ArrayStore.insertTrackStatements: ArrayHook.set*(array, arrayIndex, value [already on stack], source, sourceIndex)\n"
            + "// ArrayLoadValue.loadSourceTracker: PointTracker.getTracker(pointTracker)\n"
            + "ALOAD 3\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getTracker (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;\n"
            + "// ArrayLoadValue.loadSourceIndex: PointTracker.getIndex(pointTracker)\n"
            + "ALOAD 3\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getIndex (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)I\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayHook.setChar ([CICLbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V\n"
            + "// end ArrayStore.insertTrackStatements\n"
            + "GOTO L0\n"
            + "L1\n"
            + "FRAME FULL [$THIS$ [C I be/coekaerts/wouter/flowtracker/tracker/TrackerPoint] []\n"
            + "RETURN\n"
            + "MAXSTACK = 6\n"
            + "MAXLOCALS = 4\n");
  }

//  @Test public void tmp() {
//    testTransformClass("org.objectweb.asm.tree.analysis.Subroutine", "", "");
//  }

  /**
   * Storing in a boolean array also uses BASTORE; that should not be confused with storing in a
   * byte array
   */
  @Test
  public void booleanArrayStore() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    void t(boolean[] bs) {
                      bs[0] = true;
                    }
                  },
        "ALOAD 1\n"
            + "ICONST_0\n"
            + "ICONST_1\n"
            + "BASTORE\n"
            + "RETURN\n"
            + "MAXSTACK = 3\n"
            + "MAXLOCALS = 2\n",
        "ALOAD 1\n"
            + "ICONST_0\n"
            + "ICONST_1\n"
            + "BASTORE\n"
            + "RETURN\n"
            + "MAXSTACK = 3\n"
            + "MAXLOCALS = 2\n");
  }

  /** Test Instrumentation using {@link InvocationReturnValue} */
  @Test
  public void invocationReturnValue() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    void t(byte[] bytes) throws IOException {
                      bytes[1] = (byte) inputStream.read();
                    }
                  },
        // original code
        "ALOAD 1\n"
            + "ICONST_1\n"
            + "GETSTATIC $THISTEST$.inputStream : Ljava/io/InputStream;\n"
            + "INVOKEVIRTUAL java/io/InputStream.read ()I\n"
            + "I2B\n"
            + "BASTORE\n"
            + "RETURN\n"
            + "MAXSTACK = 3\n"
            + "MAXLOCALS = 2\n",
        // transformed code
        "// Initialize newLocal InvocationReturnValue invocation\n"
            + "ACONST_NULL\n"
            + "ASTORE 2\n"
            + "ALOAD 1\n"
            + "ICONST_1\n"
            + "GETSTATIC $THISTEST$.inputStream : Ljava/io/InputStream;\n"
            + "// InvocationReturnValue.insertTrackStatements\n"
            + "LDC \"read ()I\"\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/ShadowStack.calling (Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;\n"
            + "ASTORE 2\n"
            + "INVOKEVIRTUAL java/io/InputStream.read ()I\n"
            + "I2B\n"
            + "// begin ArrayStore.insertTrackStatements: ArrayHook.set*(array, arrayIndex, value [already on stack], source, sourceIndex)\n"
            + "// InvocationReturnValue.loadSourceTracker\n"
            + "ALOAD 2\n"
            + "GETFIELD be/coekaerts/wouter/flowtracker/tracker/Invocation.returnTracker : Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;\n"
            + "// InvocationReturnValue.loadSourceIndex\n"
            + "ALOAD 2\n"
            + "GETFIELD be/coekaerts/wouter/flowtracker/tracker/Invocation.returnIndex : I\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayHook.setByte ([BIBLbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V\n"
            + "// end ArrayStore.insertTrackStatements\n"
            + "RETURN\n"
            + "MAXSTACK = 6\n"
            + "MAXLOCALS = 3\n");
  }

  /** Test Instrumentation using {@link InvocationReturnStore} */
  @Test
  public void invocationReturnStore() {
    testTransform(new Object() {
                    @SuppressWarnings("unused")
                    int read(byte[] bytes) {
                      return bytes[1];
                    }
                  },
        // original code
        "ALOAD 1\n"
            + "ICONST_1\n"
            + "BALOAD\n"
            + "IRETURN\n"
            + "MAXSTACK = 2\n"
            + "MAXLOCALS = 2\n",
        // transformed code
        "// Initialize newLocal ArrayLoadValue array\n"
            + "ACONST_NULL\n"
            + "ASTORE 2\n"
            + "// Initialize newLocal ArrayLoadValue index\n"
            + "LDC -1\n"
            + "ISTORE 3\n"
            + "// Initialize newLocal ArrayLoadValue PointTracker\n"
            + "ACONST_NULL\n"
            + "ASTORE 4\n"
            + "// Initialize newLocal InvocationTransformation invocation\n"
            + "LDC \"read ([B)I\"\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/ShadowStack.start (Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;\n"
            + "ASTORE 5\n"
            + "ALOAD 1\n"
            + "ICONST_1\n"
            + "// begin ArrayLoadValue.insertTrackStatements\n"
            + "ISTORE 3\n"
            + "ASTORE 2\n"
            + "ALOAD 2\n"
            + "ILOAD 3\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/hook/ArrayLoadHook.getElementTracker (Ljava/lang/Object;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;\n"
            + "ASTORE 4\n"
            + "ALOAD 2\n"
            + "ILOAD 3\n"
            + "// end ArrayLoadValue.insertTrackStatements\n"
            + "BALOAD\n"
            + "// begin InvocationReturnStore.insertTrackStatements\n"
            + "ALOAD 5\n"
            + "// ArrayLoadValue.loadSourceTracker: PointTracker.getTracker(pointTracker)\n"
            + "ALOAD 4\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getTracker (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;\n"
            + "// ArrayLoadValue.loadSourceIndex: PointTracker.getIndex(pointTracker)\n"
            + "ALOAD 4\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/TrackerPoint.getIndex (Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)I\n"
            + "INVOKESTATIC be/coekaerts/wouter/flowtracker/tracker/Invocation.returning (Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V\n"
            + "// end InvocationReturnStore.insertTrackStatements\n"
            + "IRETURN\n"
            + "MAXSTACK = 4\n"
            + "MAXLOCALS = 6\n");
  }

  /**
   * Given an object of a class that contains one method, tests if the code before and after
   * transformation are as expected;
   */
  static void testTransform(Object o, String expectOriginalCode, String expectedTransformedCode) {
    testTransformClass(o.getClass().getName(), expectOriginalCode, expectedTransformedCode);
  }

  static void testTransformClass(String className, String expectOriginalCode,
      String expectedTransformedCode) {
    ClassWriter classWriter = new ClassWriter(0);
    StringWriter verifyStringWriter = new StringWriter();
    PrintWriter verifyPrintWriter = new PrintWriter(verifyStringWriter);

    String thisName = className.replace('.', '/');

    // writes out bytecode to text after transformation
    MethodPrintingClassVisitor afterVisitor =
        new MethodPrintingClassVisitor(new CheckClassAdapter(classWriter), thisName);
    ClassVisitor transformingVisitor =
        new FlowAnalyzingTransformer(new RealCommentator()).createClassAdapter(afterVisitor);
    // writes out original bytecode to text
    MethodPrintingClassVisitor beforeVisitor =
        new MethodPrintingClassVisitor(transformingVisitor, thisName);

    try {
      new ClassReader(className)
          .accept(beforeVisitor, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // verify bytecode using asm. this is not as thorough as the jvm, but gives more helpful error
    // messages when it fails
    CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), false, verifyPrintWriter);
    assertEquals("", verifyStringWriter.toString());

    try {
      verifyBytecodeJvm(className, classWriter.toByteArray());
    } catch (VerifyError e) {
      throw new AssertionError("Verification failed. Original code:\n" + beforeVisitor.getCode()
          + "\nTransformed code:\n" + afterVisitor.getCode(), e);
    }

    assertEquals(expectOriginalCode, beforeVisitor.getCode());
    assertEquals(expectedTransformedCode, afterVisitor.getCode());
  }

  /**
   * Load a class (in a new classloader), to make the JVM verify its bytecode.
   *
   * @throws VerifyError if the bytecode is not valid
   */
  private static void verifyBytecodeJvm(String className, byte[] toVerify) {
    ClassLoader classLoader = new ClassLoader(null) {
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals(className)) {
          return defineClass(name, toVerify, 0, toVerify.length);
        }
        return super.findClass(name);
      }
    };
    try {
      // getDeclaredMethods() is called to trigger bytecode verification
      //noinspection ResultOfMethodCallIgnored
      classLoader.loadClass(className).getDeclaredMethods();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * ClassVisitor that only sends the implementation of a method (not the rest of the class content)
   * to a Textifier.
   */
  private static class MethodPrintingClassVisitor extends ClassVisitor {

    final Textifier textifier = new CommentTextifier();
    final String thisName;

    public MethodPrintingClassVisitor(ClassVisitor cv, String thisName) {
      super(Opcodes.ASM9, cv);
      this.thisName = thisName;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      // skip the constructor, that's not the method we're interested in
      if (name.equals("<init>")) {
        return null;
      }

      MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
      return new TraceMethodVisitor(mv, textifier);
    }

    String getCode() {
      StringWriter sw = new StringWriter();
      textifier.print(new PrintWriter(sw));
      // remove indentation that's added by asm, but is only useful if the method implementation is
      // printed in the context of a whole class
      return sw.toString().replaceAll("^ +", "")
          .replaceAll("\n +", "\n")
          .replace(thisName, "$THIS$")
          .replace("be/coekaerts/wouter/flowtracker/weaver/flow/FlowAnalyzingTransformerTest",
              "$THISTEST$");
    }
  }
}