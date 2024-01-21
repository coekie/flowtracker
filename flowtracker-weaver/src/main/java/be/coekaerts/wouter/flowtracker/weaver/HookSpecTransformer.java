package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.FileChannelImplHook;
import be.coekaerts.wouter.flowtracker.hook.FileInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.FileOutputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.IOUtilHook;
import be.coekaerts.wouter.flowtracker.hook.InflaterInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.OutputStreamWriterHook;
import be.coekaerts.wouter.flowtracker.hook.URLConnectionHook;
import be.coekaerts.wouter.flowtracker.hook.ZipFileHook;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

class HookSpecTransformer implements ClassAdapterFactory {
  private final Map<String, ClassHookSpec> specs = new HashMap<>();

  HookSpecTransformer() {
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
        "void afterRead1(int,java.io.FileDescriptor,be.coekaerts.wouter.flowtracker.tracker.Invocation)",
        fileInputStreamFd, HookSpec.INVOCATION);
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
        "void afterWrite1(java.io.FileDescriptor,int,be.coekaerts.wouter.flowtracker.tracker.Invocation)",
        fileOutputStreamFd, HookSpec.ARG0, HookSpec.INVOCATION);
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
    // for JDK<=17
    fileChannelSpec.addMethodHookSpec("void <init>(java.io.FileDescriptor,java.lang.String,"
            + "boolean,boolean,boolean,java.lang.Object)",
        "void afterInit(java.io.FileDescriptor,java.lang.String,boolean,boolean)",
        fileChannelFd, HookSpec.ARG1, HookSpec.ARG2, HookSpec.ARG3);
    // for JDK>=21
    fileChannelSpec.addMethodHookSpec("void <init>(java.io.FileDescriptor,java.lang.String,"
            + "boolean,boolean,boolean,java.io.Closeable)",
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
        "void afterRead1(int,java.util.zip.InflaterInputStream,be.coekaerts.wouter.flowtracker.tracker.Invocation)",
        HookSpec.THIS, HookSpec.INVOCATION);
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

  @Override
  public ClassVisitor createClassAdapter(String className, ClassVisitor cv) {
    ClassHookSpec spec = getSpec(className);
    return spec == null ? cv : spec.createClassAdapter(className, cv);
  }
}
