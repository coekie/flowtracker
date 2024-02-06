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
        : new String[]{""}; // by default instrument everything
    dumpByteCodePath = config.containsKey("dumpByteCode")
        ? new File(config.get("dumpByteCode"))
        : null;
    dumpTextPath = config.containsKey("dumpText")
        ? new File(config.get("dumpText"))
        : null;
    dumpTextPrefix = config.getOrDefault("dumpTextPrefix", "");
    this.config = config;
    hookSpecTransformer = GeneratedHookSpecs.createTransformer();
  }

  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    try {
      Transformer adapterFactory = getAdapterFactory(loader, className);
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

      ClassVisitor adapter = adapterFactory.transform(className, wrappedWriter);
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
    Transformer transformer =
        getAdapterFactory(clazz.getClassLoader(), Type.getInternalName(clazz));
    return transformer != null
        && transformer.transform(Type.getInternalName(clazz), null) != null;
  }

  private Transformer getAdapterFactory(ClassLoader classLoader, String className) {
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
    // exclude the classloader that loaded flowtracker classes, or that loaded our dependencies.
    // when using flowtracker-agent-dev those are two different classloaders.
    if (classLoader == AsmTransformer.class.getClassLoader()
        || classLoader == Opcodes.class.getClassLoader()) {
      return null;
    }

    Transformer result = hookSpecTransformer;
    if (shouldAnalyze(className)) {
      result = Transformer.and(result, new SuspendInvocationTransformer());
      if (dumpTextPath == null || !className.startsWith(dumpTextPrefix)) {
        result = Transformer.and(result, new FlowAnalyzingTransformer());
      } else {
        // if we're dumping the text, then use RealCommentator to instrument it, so that the dumped
        // text includes comments
        FlowAnalyzingTransformer flowTransformer
            = new FlowAnalyzingTransformer(new RealCommentator(), new AnalysisListener());
        result = Transformer.and(result,
            new DumpTextTransformer(flowTransformer, dumpTextPath));
      }
    }
    return result;
  }

  private String outerName(String className) {
    int index = className.indexOf('$');
    return index == -1 ? className : className.substring(0, index);
  }

  private boolean shouldAnalyze(String className) {
    String outerName = outerName(className);

    if (outerName.equals("java/util/Arrays")
        || outerName.startsWith("java/lang/String") // String and friends like StringLatin1
        || outerName.equals("java/lang/AbstractStringBuilder")
        || outerName.equals("java/lang/StringBuilder")
        || outerName.equals("java/lang/StringBuffer")
        || outerName.equals("java/io/BufferedWriter")
        || outerName.equals("java/io/BufferedOutputStream")
        || outerName.equals("java/io/ByteArrayInputStream")
        || outerName.equals("java/io/ByteArrayOutputStream")
        || outerName.equals("java/io/InputStreamReader")
        || outerName.equals("java/io/OutputStreamWriter")
        || outerName.equals("java/nio/ByteBuffer")
        || outerName.equals("java/nio/HeapByteBuffer")
        || outerName.equals("java/nio/HeapCharBuffer")
        || outerName.equals("sun/nio/cs/UTF_8")
        || outerName.equals("sun/nio/cs/StreamDecoder")
        || outerName.equals("sun/nio/cs/StreamEncoder")
        || outerName.equals("sun/nio/ch/NioSocketImpl")
        || outerName.equals("java/net/SocketInputStream") // JDK 11
        || outerName.equals("java/net/SocketOutputStream") // JDK 11
        || outerName.equals("java/net/Socket")
        || outerName.equals("java/lang/ClassLoader")
        || outerName.startsWith("com/sun/org/apache/xerces")
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
