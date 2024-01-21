package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.util.Logger;
import be.coekaerts.wouter.flowtracker.weaver.debug.DumpTextTransformer;
import be.coekaerts.wouter.flowtracker.weaver.debug.RealCommentator;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.AnalysisListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

class AsmTransformer implements ClassFileTransformer {
  private static final Logger logger = new Logger("AsmTransformer");

  private final String[] packagesToInstrument;
  private final File dumpByteCodePath;
  private final File dumpTextPath;
  private final String dumpTextPrefix;
  private final Map<String, String> config;
  private final HookSpecTransformer hookSpecTransformer;

  public AsmTransformer(Map<String, String> config) {
    this.packagesToInstrument = config.containsKey("packages")
        ? config.get("packages").split(",")
        : new String[0];
    dumpByteCodePath = config.containsKey("dumpByteCode")
        ? new File(config.get("dumpByteCode"))
        : null;
    dumpTextPath = config.containsKey("dumpText")
        ? new File(config.get("dumpText"))
        : null;
    dumpTextPrefix = config.getOrDefault("dumpTextPrefix", "");
    this.config = config;
    hookSpecTransformer = new HookSpecTransformer();
  }

  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    try {
      ClassAdapterFactory adapterFactory = getAdapterFactory(loader, className);
      if (adapterFactory == null) {
        return null;
      }

      ClassWriter writer = new ClassWriter(0);

      // NICE make checkAdapter optional; for development only
      // wrap with extra ClassVisitor as workaround for CheckClassAdapter (since ASM 9.4) having
      // issues dealing with JDK classes that are being redefined but missing StackMapFrames because
      // of https://bugs.openjdk.org/browse/JDK-8228604 .
      // this avoids the "instanceof ClassWriter" check in CheckClassAdapter, so that is skips
      // verifying the frames
      ClassVisitor wrappedWriter = new CheckClassAdapter(new ClassVisitor(Opcodes.ASM9, writer) {});

      ClassVisitor adapter = adapterFactory.createClassAdapter(className, wrappedWriter);
      if (className.equals("java/lang/String")) {
        adapter = new StringAdapter(adapter, config);
      }

      // optimization: if we aren't changing anything, then don't process the class
      if (adapter == wrappedWriter) {
        return null;
      }

      new ClassReader(classfileBuffer).accept(adapter, ClassReader.EXPAND_FRAMES);
      byte[] result = writer.toByteArray();

      maybeDumpByteCode(className, result);

      logger.info("Transformed %s", className);
      return result;
    } catch (Throwable t) {
      logger.error(t, "Exception transforming %s", className);
      throw new RuntimeException("Exception transforming class " + className, t);
    }
  }

  boolean shouldRetransformOnStartup(Class<?> clazz, Instrumentation instrumentation) {
    if (!instrumentation.isModifiableClass(clazz)) {
      return false;
    }
    ClassAdapterFactory transformer =
        getAdapterFactory(clazz.getClassLoader(), Type.getInternalName(clazz));
    return transformer != null
        && transformer.createClassAdapter(Type.getInternalName(clazz), null) != null;
  }

  private ClassAdapterFactory getAdapterFactory(ClassLoader classLoader, String className) {
    // don't transform classes without a name,
    // e.g. classes created at runtime through Unsafe.defineAnonymousClass
    if (className == null) {
      return null;
    }

    // don't transform array classes
    if (className.charAt(0) == '[') {
      return null;
    }

    // never transform our own classes
    if (className.startsWith("be/coekaerts/wouter/flowtracker")
        && !className.startsWith("be/coekaerts/wouter/flowtracker/test")) {
      return null;
    }
    if (classLoader == AsmTransformer.class.getClassLoader()) {
      return null;
    }

    ClassAdapterFactory result = hookSpecTransformer;
    if (shouldAnalyze(className)) {
      result = ClassAdapterFactory.and(result, new SuspendInvocationTransformer());
      if (dumpTextPath == null || !className.startsWith(dumpTextPrefix)) {
        result = ClassAdapterFactory.and(result, new FlowAnalyzingTransformer());
      } else {
        // if we're dumping the text, then use RealCommentator to instrument it, so that the dumped
        // text includes comments
        FlowAnalyzingTransformer flowTransformer
            = new FlowAnalyzingTransformer(new RealCommentator(), new AnalysisListener());
        result = ClassAdapterFactory.and(result,
            new DumpTextTransformer(flowTransformer, dumpTextPath));
      }
    }
    return result;
  }

  private boolean shouldAnalyze(String className) {
    if (className.equals("java/util/Arrays")
        || className.startsWith("java/lang/String") // String and friends like StringLatin1
        || className.equals("java/lang/AbstractStringBuilder")
        || className.equals("java/lang/StringBuilder")
        || className.equals("java/lang/StringBuffer")
        || className.equals("java/io/BufferedWriter")
        || className.equals("java/io/BufferedOutputStream")
        || className.equals("java/io/ByteArrayInputStream")
        || className.equals("java/io/ByteArrayOutputStream")
        || className.equals("java/io/InputStreamReader")
        || className.equals("java/io/OutputStreamWriter")
        || className.equals("java/nio/HeapCharBuffer")
        || className.equals("sun/nio/cs/UTF_8$Encoder")
        || className.equals("sun/nio/cs/UTF_8$Decoder")
        || className.equals("sun/nio/cs/StreamDecoder")
        || className.equals("sun/nio/cs/StreamEncoder")
        || className.equals("java/lang/ClassLoader")
        || className.startsWith("com/sun/org/apache/xerces")
    ) {
      return true;
    }

    for (String packageToInstrument : packagesToInstrument) {
      if (className.startsWith(packageToInstrument)) {
        return true;
      }
    }
    return false;
  }

  /** If enabled in configuration, write the generated bytecode to a file, for debugging */
  private void maybeDumpByteCode(String className, byte[] bytes) {
    if (dumpByteCodePath != null) {
      try {
        String fileName = className.replaceAll("[/.$]", "_") + ".class";
        try (FileOutputStream out = new FileOutputStream(new File(dumpByteCodePath, fileName))) {
          out.write(bytes);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
