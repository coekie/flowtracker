package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.util.Config;
import be.coekaerts.wouter.flowtracker.util.Logger;
import be.coekaerts.wouter.flowtracker.weaver.debug.DumpTextTransformer;
import be.coekaerts.wouter.flowtracker.weaver.debug.RealCommentator;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

class AsmTransformer implements ClassFileTransformer {
  private static final Logger logger = new Logger("AsmTransformer");
  private static final String RECOMMENDED_FILTER = "+java.util.Arrays,"
      + "+java.lang.String*," // String and friends like StringBuilder, StringLatin1
      + "+java.lang.AbstractStringBuilder,"
      + "+java.io.BufferedWriter,"
      + "+java.io.BufferedOutputStream,"
      + "+java.io.ByteArrayInputStream,"
      + "+java.io.ByteArrayOutputStream,"
      + "+java.io.InputStreamReader,"
      + "+java.io.OutputStreamWriter,"
      + "+java.nio.ByteBuffer,"
      + "+java.nio.HeapByteBuffer,"
      + "+java.nio.HeapCharBuffer,"
      + "+sun.nio.cs.UTF_8,"
      + "+sun.nio.cs.StreamDecoder,"
      + "+sun.nio.cs.StreamEncoder,"
      + "+sun.nio.ch.NioSocketImpl,"
      + "+java.net.SocketInputStream," // JDK 11
      + "+java.net.SocketOutputStream," // JDK 11
      + "+java.net.Socket,"
      + "+java.lang.ClassLoader,"
      + "+com.sun.org.apache.xerces.*,"
      + "-java.lang.CharacterData*"; // seems to break the debugger sometimes?
  private static final String DEFAULT_FILTER = "-java.lang.CharacterData*,+*";

  private final ClassFilter toInstrumentFilter;
  private final File dumpByteCodePath;
  private final File dumpTextPath;
  private final String dumpTextPrefix;
  private final Config config;
  private final HookSpecTransformer hookSpecTransformer;
  private final FlowAnalyzingTransformer flowAnalyzingTransformer;

  public AsmTransformer(Config config) {
    toInstrumentFilter = new ClassFilter(config.get("filter", DEFAULT_FILTER), RECOMMENDED_FILTER);
    dumpByteCodePath = config.containsKey("dumpByteCode")
        ? new File(config.get("dumpByteCode"))
        : null;
    dumpTextPath = config.containsKey("dumpText")
        ? new File(config.get("dumpText"))
        : null;
    dumpTextPrefix = config.get("dumpTextPrefix", "");
    this.config = config;
    hookSpecTransformer = GeneratedHookSpecs.createTransformer();
    flowAnalyzingTransformer = new FlowAnalyzingTransformer(config);
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
    // (when using flowtracker-agent-dev those are two different classloaders; when running from
    // real jar, those are the same.)
    if (classLoader == AsmTransformer.class.getClassLoader()
        || classLoader == Opcodes.class.getClassLoader()) {
      return null;
    }

    Transformer result = hookSpecTransformer;
    if (toInstrumentFilter.include(className)) {
      result = Transformer.and(result, new SuspendInvocationTransformer());
      if (dumpTextPath == null || !className.startsWith(dumpTextPrefix)) {
        result = Transformer.and(result, flowAnalyzingTransformer);
      } else {
        // if we're dumping the text, then use RealCommentator to instrument it, so that the dumped
        // text includes comments
        FlowAnalyzingTransformer flowTransformer
            = new FlowAnalyzingTransformer(config, new RealCommentator());
        result = Transformer.and(result,
            new DumpTextTransformer(flowTransformer, dumpTextPath));
      }
    }
    return result;
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
