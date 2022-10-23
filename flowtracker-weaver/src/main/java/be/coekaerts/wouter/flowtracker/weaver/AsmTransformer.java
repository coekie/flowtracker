package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.FileInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.InputStreamReaderHook;
import be.coekaerts.wouter.flowtracker.hook.OutputStreamWriterHook;
import be.coekaerts.wouter.flowtracker.hook.URLConnectionHook;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class AsmTransformer implements ClassFileTransformer {
  private final Map<String, ClassHookSpec> specs = new HashMap<>();
  private final String[] packagesToInstrument;
  private final File dumpByteCodePath;
  private final Map<String, String> config;

  public AsmTransformer(Map<String, String> config) {
    this.packagesToInstrument = config.containsKey("packages")
        ? config.get("packages").split(",")
        : new String[0];
    dumpByteCodePath = config.containsKey("dumpByteCode")
        ? new File(config.get("dumpByteCode"))
        : null;
    this.config = config;
    ClassHookSpec inputStreamReaderSpec =
        new ClassHookSpec(Type.getType("Ljava/io/InputStreamReader;"), InputStreamReaderHook.class);
    inputStreamReaderSpec.addMethodHookSpec("void <init>(java.io.InputStream)",
        "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec("void <init>(java.io.InputStream, java.lang.String)",
        "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec(
        "void <init>(java.io.InputStream, java.nio.charset.Charset)",
        "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec(
        "void <init>(java.io.InputStream, java.nio.charset.CharsetDecoder)",
        "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec("int read()",
        "void afterRead1(int,java.io.InputStreamReader)", HookSpec.THIS);
    inputStreamReaderSpec.addMethodHookSpec("int read(char[],int,int)",
        "void afterReadCharArrayOffset(int,java.io.InputStreamReader,char[],int)",
        HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1);
    inputStreamReaderSpec.addMethodHookSpec("int read(java.nio.CharBuffer)",
        "void afterReadCharBuffer(int,java.io.InputStreamReader,java.nio.CharBuffer)",
        HookSpec.THIS, HookSpec.ARG0);
    // we assume the other methods ultimately delegate to the ones we hooked
    specs.put("java/io/InputStreamReader", inputStreamReaderSpec);

    ClassHookSpec outputStreamWriterSpec = new ClassHookSpec(
        Type.getType("Ljava/io/OutputStreamWriter;"), OutputStreamWriterHook.class);
    outputStreamWriterSpec.addMethodHookSpec("void <init>(java.io.OutputStream)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void <init>(java.io.OutputStream,java.lang.String)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec(
        "void <init>(java.io.OutputStream,java.nio.charset.Charset)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec(
        "void <init>(java.io.OutputStream,java.nio.charset.CharsetEncoder)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void write(int)",
        "void afterWrite1(java.io.OutputStreamWriter, int)", HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void write(char[],int,int)",
        "void afterWriteCharArrayOffset(java.io.OutputStreamWriter,char[],int,int)",
        HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    outputStreamWriterSpec.addMethodHookSpec("void write(java.lang.String,int,int)",
        "void afterWriteStringOffset(java.io.OutputStreamWriter,java.lang.String,int,int)",
        HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    specs.put("java/io/OutputStreamWriter", outputStreamWriterSpec);

    ClassHookSpec fileInputStreamSpec = new ClassHookSpec(Type.getType("Ljava/io/FileInputStream;"),
        FileInputStreamHook.class);
    fileInputStreamSpec.addMethodHookSpec("void <init>(java.io.File)",
        "void afterInit(java.io.FileInputStream,java.io.File)", HookSpec.THIS, HookSpec.ARG0);
    fileInputStreamSpec.addMethodHookSpec("int read()",
        "void afterRead1(int,java.io.FileInputStream)", HookSpec.THIS);
    fileInputStreamSpec.addMethodHookSpec("int read(byte[])",
        "void afterReadByteArray(int,java.io.FileInputStream,byte[])", HookSpec.THIS, HookSpec.ARG0);
    fileInputStreamSpec.addMethodHookSpec("int read(byte[],int,int)",
        "void afterReadByteArrayOffset(int,java.io.FileInputStream,byte[],int)", HookSpec.THIS,
        HookSpec.ARG0, HookSpec.ARG1);
    specs.put("java/io/FileInputStream", fileInputStreamSpec);
  }

  private ClassHookSpec getSpec(String className) {
    if (className.endsWith("URLConnection")) {
      return urlConnectionHook(className);
    }
    return specs.get(className);
  }

  private ClassHookSpec urlConnectionHook(String urlConnectionSubclass) {
    ClassHookSpec spec = new ClassHookSpec(
        Type.getType('L' + urlConnectionSubclass.replace('.', '/') + ';'), URLConnectionHook.class);
    spec.addMethodHookSpec("java.io.InputStream getInputStream()",
        "void afterGetInputStream(java.io.InputStream,java.net.URLConnection)", HookSpec.THIS);
    return spec;
  }

  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    try {
      ClassAdapterFactory adapterFactory = getAdapterFactory(loader, className);
      if (adapterFactory == null) {
        return null;
      }

      ClassReader reader = new ClassReader(classfileBuffer);
      ClassWriter writer = new ClassWriter(0);
      // NICE make checkAdapter optional; for development only
      CheckClassAdapter checkAdapter = new CheckClassAdapter(writer);
      ClassVisitor adapter = adapterFactory.createClassAdapter(checkAdapter);
      if (className.equals("java/lang/String")) {
        adapter = new StringAdapter(adapter, config);
      }
      reader.accept(adapter, ClassReader.EXPAND_FRAMES);
      byte[] result = writer.toByteArray();

      maybeDumpByteCode(className, result);

      System.out.println("AsmTransformer: Transformed " + className);
      return result;
    } catch (Throwable t) {
      // TODO better logging
      System.err.println("Exception transforming " + className);
      t.printStackTrace(); // make sure the exception isn't silently ignored
      throw new RuntimeException("Exception while transforming class", t);
    }
  }

  // called using reflection from FlowTrackAgent
  public boolean shouldRetransformOnStartup(Class<?> clazz, Instrumentation instrumentation) {
    return getAdapterFactory(clazz.getClassLoader(), Type.getInternalName(clazz)) != null
        && instrumentation.isModifiableClass(clazz);
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

    ClassHookSpec spec = getSpec(className);
    if (spec != null) {
      return spec;
    } else if (shouldAnalyze(className)) {
      return new FlowAnalyzingTransformer();
    } else {
      return null;
    }
  }

  private boolean shouldAnalyze(String className) {
    if (className.equals("java/util/Arrays")
        || className.startsWith("java/lang/String") // String and friends like StringLatin1
        || className.equals("java/lang/AbstractStringBuilder")
        || className.equals("java/io/BufferedWriter")) {
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
