package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.FileChannelImplHook;
import be.coekaerts.wouter.flowtracker.hook.FileInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.FileOutputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.IOUtilHook;
import be.coekaerts.wouter.flowtracker.hook.InflaterInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.OutputStreamWriterHook;
import be.coekaerts.wouter.flowtracker.hook.URLConnectionHook;
import be.coekaerts.wouter.flowtracker.hook.ZipFileHook;
import be.coekaerts.wouter.flowtracker.util.Logger;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
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

class AsmTransformer implements ClassFileTransformer {
  private static final Logger logger = new Logger("AsmTransformer");

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
    HookArgument fileInputStreamFd = HookSpec.field(Type.getType("Ljava/io/FileInputStream;"), "fd",
        Type.getType("Ljava/io/FileDescriptor;"));
    fileInputStreamSpec.addMethodHookSpec("void <init>(java.io.File)",
        "void afterInit(java.io.FileDescriptor,java.io.File)", fileInputStreamFd, HookSpec.ARG0);
    fileInputStreamSpec.addMethodHookSpec("int read()",
        "void afterRead1(int,java.io.FileDescriptor)", fileInputStreamFd);
    fileInputStreamSpec.addMethodHookSpec("int read(byte[])",
        "void afterReadByteArray(int,java.io.FileDescriptor,byte[])",
        fileInputStreamFd, HookSpec.ARG0);
    fileInputStreamSpec.addMethodHookSpec("int read(byte[],int,int)",
        "void afterReadByteArrayOffset(int,java.io.FileDescriptor,byte[],int)",
        fileInputStreamFd, HookSpec.ARG0, HookSpec.ARG1);
    specs.put("java/io/FileInputStream", fileInputStreamSpec);

    ClassHookSpec fileOutputStreamSpec =
        new ClassHookSpec(Type.getType("Ljava/io/FileOutputStream;"), FileOutputStreamHook.class);
    HookArgument fileOutputStreamFd = HookSpec.field(Type.getType("Ljava/io/FileOutputStream;"),
        "fd", Type.getType("Ljava/io/FileDescriptor;"));
    fileOutputStreamSpec.addMethodHookSpec("void <init>(java.io.File,boolean)",
        "void afterInit(java.io.FileDescriptor,java.io.File)", fileOutputStreamFd, HookSpec.ARG0);
    fileOutputStreamSpec.addMethodHookSpec("void write(int)",
        "void afterWrite1(java.io.FileDescriptor,int)", fileOutputStreamFd, HookSpec.ARG0);
    fileOutputStreamSpec.addMethodHookSpec("void write(byte[])",
        "void afterWriteByteArray(java.io.FileDescriptor,byte[])",
        fileOutputStreamFd, HookSpec.ARG0);
    fileOutputStreamSpec.addMethodHookSpec("void write(byte[],int,int)",
        "void afterWriteByteArrayOffset(java.io.FileDescriptor,byte[],int,int)",
        fileOutputStreamFd, HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    specs.put("java/io/FileOutputStream", fileOutputStreamSpec);

    ClassHookSpec fileChannelSpec = new ClassHookSpec(Type.getType("Lsun/nio/ch/FileChannelImpl;"),
        FileChannelImplHook.class);
    HookArgument fileChannelFd = HookSpec.field(Type.getType("Lsun/nio/ch/FileChannelImpl;"), "fd",
        Type.getType("Ljava/io/FileDescriptor;"));
    fileChannelSpec.addMethodHookSpec("void <init>(java.io.FileDescriptor,java.lang.String,"
            + "boolean,boolean,boolean,java.lang.Object)",
        "void afterInit(java.io.FileDescriptor,java.lang.String,boolean,boolean)",
        fileChannelFd, HookSpec.ARG1, HookSpec.ARG2, HookSpec.ARG3);
    specs.put("sun/nio/ch/FileChannelImpl", fileChannelSpec);

    ClassHookSpec ioUtilSpec = new ClassHookSpec(Type.getType("Lsun/nio/ch/IOUtil;"),
        IOUtilHook.class);
    // between JDK 11 and 17 an extra argument "async" was added
    if (Runtime.version().feature() >= 17) {
      ioUtilSpec.addMethodHookSpec("int read(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterReadByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
      ioUtilSpec.addMethodHookSpec("int write(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterWriteByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    } else {
      ioUtilSpec.addMethodHookSpec("int read(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterReadByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
      ioUtilSpec.addMethodHookSpec("int write(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterWriteByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    }
    specs.put("sun/nio/ch/IOUtil", ioUtilSpec);

    ClassHookSpec inflaterInputStreamSpec = new ClassHookSpec(
        Type.getType("Ljava/util/zip/InflaterInputStream;"), InflaterInputStreamHook.class);
    inflaterInputStreamSpec.addMethodHookSpec(
        "void <init>(java.io.InputStream,java.util.zip.Inflater,int)",
        "void afterInit(java.util.zip.InflaterInputStream,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inflaterInputStreamSpec.addMethodHookSpec("int read(byte[],int,int)",
        "void afterReadByteArrayOffset(int,java.util.zip.InflaterInputStream,byte[],int)",
        HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1);
    inflaterInputStreamSpec.addMethodHookSpec("int read()",
        "void afterRead1(int,java.util.zip.InflaterInputStream)",
        HookSpec.THIS);
    specs.put("java/util/zip/InflaterInputStream", inflaterInputStreamSpec);

    ClassHookSpec zipFileSpec = new ClassHookSpec(
        Type.getType("Ljava/util/zip/ZipFile;"), ZipFileHook.class);
    zipFileSpec.addMethodHookSpec("java.io.InputStream getInputStream(java.util.zip.ZipEntry)",
        "void afterGetInputStream(java.io.InputStream,java.util.zip.ZipFile,java.util.zip.ZipEntry)",
        HookSpec.THIS, HookSpec.ARG0);
    specs.put("java/util/zip/ZipFile", zipFileSpec);
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

  // not used yet, see CharsetEncoderTest
//  private ClassHookSpec charsetEncoderSpec() {
//    return new ClassHookSpec(Type.getType("Ljava/nio/charset/CharsetEncoder;"),
//            CharsetEncoderHook.class)
//        .addMethodHookSpec(
//            "java.nio.charset.CoderResult encode(java.nio.CharBuffer,java.nio.ByteBuffer,boolean)",
//            "void afterEncode(int,int,java.nio.CharBuffer,java.nio.ByteBuffer)", ...);
//  }

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

      logger.info("Transformed %s", className);
      return result;
    } catch (Throwable t) {
      logger.error(t, "Exception transforming %s", className);
      throw new RuntimeException("Exception transforming class " + className, t);
    }
  }

  boolean shouldRetransformOnStartup(Class<?> clazz, Instrumentation instrumentation) {
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
        || className.equals("java/io/BufferedWriter")
        || className.equals("java/io/BufferedOutputStream")
        || className.equals("java/io/ByteArrayOutputStream")
        || className.equals("java/nio/HeapCharBuffer")
        || className.equals("sun/nio/cs/UTF_8$Encoder")
        || className.equals("sun/nio/cs/UTF_8$Decoder")
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
