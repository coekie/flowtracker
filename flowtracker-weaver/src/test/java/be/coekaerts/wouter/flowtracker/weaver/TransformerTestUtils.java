package be.coekaerts.wouter.flowtracker.weaver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

class TransformerTestUtils {
  /**
   * Transform the class with the transformer, invoke its constructor and {@link Runnable#run()}
   */
  static void transformAndRun(ClassAdapterFactory transformer, Type className,
      Object... constructArgs)
      throws ReflectiveOperationException {
    Class<?> clazz = transformClass(transformer, className);
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    Object instance = constructor.newInstance(constructArgs);
    ((Runnable) instance).run();
  }

  private static Class<?> transformClass(ClassAdapterFactory transformer, Type classType) {
    String suffix = "2"; // transformed class named className + suffix

    ClassWriter classWriter = new ClassWriter(0);
    StringWriter verifyStringWriter = new StringWriter();
    PrintWriter verifyPrintWriter = new PrintWriter(verifyStringWriter);
    StringWriter afterStringWriter = new StringWriter();
    PrintWriter afterPrintWriter = new PrintWriter(afterStringWriter);
    StringWriter beforeStringWriter = new StringWriter();
    PrintWriter beforePrintWriter = new PrintWriter(beforeStringWriter);

    // writes out bytecode to text after transformation
    TraceClassVisitor afterVisitor =
        new TraceClassVisitor(new CheckClassAdapter(classWriter), afterPrintWriter);
    ClassVisitor transformingVisitor =
        transformer.createClassAdapter(classType.getInternalName(), afterVisitor);
    // writes out original bytecode to text
    TraceClassVisitor beforeVisitor =
        new TraceClassVisitor(transformingVisitor, beforePrintWriter);
    ClassRemapper remapper = new ClassRemapper(beforeVisitor,
        new SimpleRemapper(
            Map.of(classType.getInternalName(), classType.getInternalName() + suffix)));

    try {
      new ClassReader(classType.getClassName())
          .accept(remapper, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // verify bytecode using asm. this is not as thorough as the jvm, but gives more helpful error
    // messages when it fails
    CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), false, verifyPrintWriter);
    assertEquals("", verifyStringWriter.toString());

    try {
      return loadClass(classType.getClassName() + suffix, classWriter.toByteArray());
    } catch (VerifyError e) {
      throw new AssertionError("Verification failed. Original code:\n" + beforeStringWriter
          + "\nTransformed code:\n" + afterStringWriter, e);
    }
  }

  /**
   * Load a class in a child classloader.
   */
  private static Class<?> loadClass(String className, byte[] byteCode) {
    ClassLoader classLoader = new ClassLoader(TransformerTestUtils.class.getClassLoader()) {
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals(className)) {
          return defineClass(name, byteCode, 0, byteCode.length);
        }
        return super.findClass(name);
      }
    };
    try {
      return classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
