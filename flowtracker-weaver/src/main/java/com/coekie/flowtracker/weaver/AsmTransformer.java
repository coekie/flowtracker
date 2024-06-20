package com.coekie.flowtracker.weaver;

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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.util.Config;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.weaver.debug.DumpTextTransformer;
import com.coekie.flowtracker.weaver.debug.RealCommentator;
import com.coekie.flowtracker.weaver.flow.FlowTransformer;
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
  private static final String BASE_FILTER = "+java.util.Arrays,"
      + "+java.lang.String*," // String and friends like StringBuilder, StringLatin1
      + "+java.lang.AbstractStringBuilder,"
      + "+java.lang.Character,"
      + "+java.lang.invoke.StringConcatFactory,"
      + "+java.io.Bits," // JDK 11
      + "+java.io.BufferedWriter,"
      + "+java.io.BufferedOutputStream,"
      + "+java.io.ByteArrayInputStream,"
      + "+java.io.ByteArrayOutputStream,"
      + "+java.io.DataOutputStream,"
      + "+java.io.InputStreamReader,"
      + "+java.io.ObjectOutputStream,"
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
      + "-jdk.internal.misc.Unsafe,"
      + "-java.lang.CharacterData*," // seems to break the debugger sometimes?
      // causes ClassCircularityError when running with -Xverify:all, because it is used indirectly
      // by Modules.transformedByAgent
      + "-java.lang.WeakPairMap*";
  // equivalent to `%base,+*` but simpler
  private static final String DEFAULT_FILTER = "-java.lang.CharacterData*,"
      + "-jdk.internal.misc.Unsafe,"
      + "-java.lang.WeakPairMap*,"
      + "+*";

  private final ClassFilter toInstrumentFilter;
  private final File dumpByteCodePath;
  private final File dumpTextPath;
  private final String dumpTextPrefix;
  private final Config config;
  private final HookSpecTransformer hookSpecTransformer;
  private final FlowTransformer flowTransformer;

  public AsmTransformer(Config config) {
    toInstrumentFilter = new ClassFilter(config.get("filter", DEFAULT_FILTER), BASE_FILTER);
    dumpByteCodePath = config.containsKey("dumpByteCode")
        ? new File(config.get("dumpByteCode"))
        : null;
    dumpTextPath = config.containsKey("dumpText")
        ? new File(config.get("dumpText"))
        : null;
    dumpTextPrefix = config.get("dumpTextPrefix", "");
    this.config = config;
    hookSpecTransformer = GeneratedHookSpecs.createTransformer(config);
    flowTransformer = new FlowTransformer(config);
  }

  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    Invocation suspended = Invocation.suspend();
    Context context = context();
    context.suspend();
    try {
      Transformer adapterFactory = getAdapterFactory(loader, className);
      if (adapterFactory == null) {
        return null;
      }

      ClassWriter writer = new ClassWriter(0);

      // wrap with extra ClassVisitor as workaround for CheckClassAdapter (since ASM 9.4) having
      // issues dealing with JDK classes that are being redefined but missing StackMapFrames because
      // of https://bugs.openjdk.org/browse/JDK-8228604 .
      // this avoids the "instanceof ClassWriter" check in CheckClassAdapter, so that is skips
      // verifying the frames
      ClassVisitor wrappedWriter = config.getBoolean("verify", false)
          ? new CheckClassAdapter(new ClassVisitor(Opcodes.ASM9, writer) {})
          : writer;

      ClassVisitor adapter = adapterFactory.transform(loader, className, wrappedWriter);
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
    } finally {
      Invocation.unsuspend(suspended);
      context.unsuspend();
    }
  }

  boolean shouldRetransformOnStartup(Class<?> clazz, Instrumentation instrumentation) {
    if (!instrumentation.isModifiableClass(clazz)) {
      return false;
    }
    Transformer transformer =
        getAdapterFactory(clazz.getClassLoader(), Type.getInternalName(clazz));
    return transformer != null
        && transformer.transform(clazz.getClassLoader(), Type.getInternalName(clazz), null) != null;
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
    if (className.startsWith("com/coekie/flowtracker")
        && !className.startsWith("com/coekie/flowtracker/test")) {
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
        result = Transformer.and(result, flowTransformer);
      } else {
        // if we're dumping the text, then use RealCommentator to instrument it, so that the dumped
        // text includes comments
        FlowTransformer flowTransformer
            = new FlowTransformer(config, new RealCommentator());
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
