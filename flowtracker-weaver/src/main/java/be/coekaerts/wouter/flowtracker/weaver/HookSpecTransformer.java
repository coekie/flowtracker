package be.coekaerts.wouter.flowtracker.weaver;

import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG0;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG1;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG2;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG3;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.INVOCATION;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.THIS;

import be.coekaerts.wouter.flowtracker.hook.FileChannelImplHook;
import be.coekaerts.wouter.flowtracker.hook.FileInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.FileOutputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.IOUtilHook;
import be.coekaerts.wouter.flowtracker.hook.InflaterInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.NetSocketInputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.NetSocketOutputStreamHook;
import be.coekaerts.wouter.flowtracker.hook.OutputStreamWriterHook;
import be.coekaerts.wouter.flowtracker.hook.SocketChannelImplHook;
import be.coekaerts.wouter.flowtracker.hook.SocketImplHook;
import be.coekaerts.wouter.flowtracker.hook.URLConnectionHook;
import be.coekaerts.wouter.flowtracker.hook.ZipFileHook;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

class HookSpecTransformer implements Transformer {
  private final Map<String, ClassHookSpec> specs = new HashMap<>();

  HookSpecTransformer() {
    int version = Runtime.version().feature();

    ClassHookSpec outputStreamWriterSpec = new ClassHookSpec(
        "java/io/OutputStreamWriter", OutputStreamWriterHook.class);
    outputStreamWriterSpec.addMethodHookSpec("void <init>(java.io.OutputStream)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        THIS, ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void <init>(java.io.OutputStream,java.lang.String)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        THIS, ARG0);
    outputStreamWriterSpec.addMethodHookSpec(
        "void <init>(java.io.OutputStream,java.nio.charset.Charset)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        THIS, ARG0);
    outputStreamWriterSpec.addMethodHookSpec(
        "void <init>(java.io.OutputStream,java.nio.charset.CharsetEncoder)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        THIS, ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void write(int)",
        "void afterWrite1(java.io.OutputStreamWriter, int)", THIS, ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void write(char[],int,int)",
        "void afterWriteCharArrayOffset(java.io.OutputStreamWriter,char[],int,int)",
        THIS, ARG0, ARG1, ARG2);
    outputStreamWriterSpec.addMethodHookSpec("void write(java.lang.String,int,int)",
        "void afterWriteStringOffset(java.io.OutputStreamWriter,java.lang.String,int,int)",
        THIS, ARG0, ARG1, ARG2);
    register(outputStreamWriterSpec);

    ClassHookSpec fileInputStreamSpec = new ClassHookSpec(
        "java/io/FileInputStream", FileInputStreamHook.class);
    HookArgument fileInputStreamFd = HookSpec.field(Type.getType("Ljava/io/FileInputStream;"), "fd",
        Type.getType("Ljava/io/FileDescriptor;"));
    fileInputStreamSpec.addMethodHookSpec("void <init>(java.io.File)",
        "void afterInit(java.io.FileDescriptor,java.io.File)", fileInputStreamFd, ARG0);
    fileInputStreamSpec.addMethodHookSpec("int read()",
        "void afterRead1(int,java.io.FileDescriptor,be.coekaerts.wouter.flowtracker.tracker.Invocation)",
        fileInputStreamFd, INVOCATION);
    fileInputStreamSpec.addMethodHookSpec("int read(byte[])",
        "void afterReadByteArray(int,java.io.FileDescriptor,byte[])",
        fileInputStreamFd, ARG0);
    fileInputStreamSpec.addMethodHookSpec("int read(byte[],int,int)",
        "void afterReadByteArrayOffset(int,java.io.FileDescriptor,byte[],int)",
        fileInputStreamFd, ARG0, ARG1);
    register(fileInputStreamSpec);

    ClassHookSpec fileOutputStreamSpec = new ClassHookSpec(
        "java/io/FileOutputStream", FileOutputStreamHook.class);
    HookArgument fileOutputStreamFd = HookSpec.field(Type.getType("Ljava/io/FileOutputStream;"),
        "fd", Type.getType("Ljava/io/FileDescriptor;"));
    fileOutputStreamSpec.addMethodHookSpec("void <init>(java.io.File,boolean)",
        "void afterInit(java.io.FileDescriptor,java.io.File)", fileOutputStreamFd, ARG0);
    fileOutputStreamSpec.addMethodHookSpec("void write(int)",
        "void afterWrite1(java.io.FileDescriptor,int,be.coekaerts.wouter.flowtracker.tracker.Invocation)",
        fileOutputStreamFd, ARG0, INVOCATION);
    fileOutputStreamSpec.addMethodHookSpec("void write(byte[])",
        "void afterWriteByteArray(java.io.FileDescriptor,byte[])",
        fileOutputStreamFd, ARG0);
    fileOutputStreamSpec.addMethodHookSpec("void write(byte[],int,int)",
        "void afterWriteByteArrayOffset(java.io.FileDescriptor,byte[],int,int)",
        fileOutputStreamFd, ARG0, ARG1, ARG2);
    register(fileOutputStreamSpec);

    ClassHookSpec fileChannelSpec = new ClassHookSpec(
        "sun/nio/ch/FileChannelImpl", FileChannelImplHook.class);
    HookArgument fileChannelFd = HookSpec.field(Type.getType("Lsun/nio/ch/FileChannelImpl;"), "fd",
        Type.getType("Ljava/io/FileDescriptor;"));
    // for JDK<=17
    fileChannelSpec.addMethodHookSpec("void <init>(java.io.FileDescriptor,java.lang.String,"
            + "boolean,boolean,boolean,java.lang.Object)",
        "void afterInit(java.io.FileDescriptor,java.lang.String,boolean,boolean)",
        fileChannelFd, ARG1, ARG2, ARG3);
    // for JDK>=21
    fileChannelSpec.addMethodHookSpec("void <init>(java.io.FileDescriptor,java.lang.String,"
            + "boolean,boolean,boolean,java.io.Closeable)",
        "void afterInit(java.io.FileDescriptor,java.lang.String,boolean,boolean)",
        fileChannelFd, ARG1, ARG2, ARG3);
    register(fileChannelSpec);

    ClassHookSpec ioUtilSpec = new ClassHookSpec("sun/nio/ch/IOUtil", IOUtilHook.class);
    // between JDK 11 and 17 an extra argument "async" was added
    if (version >= 17) {
      ioUtilSpec.addMethodHookSpec("int read(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterReadByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          ARG0, ARG1, ARG2);
      ioUtilSpec.addMethodHookSpec("int write(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterWriteByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          ARG0, ARG1, ARG2);
    } else {
      ioUtilSpec.addMethodHookSpec("int read(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterReadByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          ARG0, ARG1, ARG2);
      ioUtilSpec.addMethodHookSpec("int write(java.io.FileDescriptor,java.nio.ByteBuffer,long,"
              + "boolean,int,sun.nio.ch.NativeDispatcher)",
          "void afterWriteByteBuffer(int,java.io.FileDescriptor,java.nio.ByteBuffer,long)",
          ARG0, ARG1, ARG2);
    }
    register(ioUtilSpec);

    // JDK 17+
    ClassHookSpec nioSocketImplSpec =
        new ClassHookSpec("sun/nio/ch/NioSocketImpl", SocketImplHook.class);
    HookArgument socketImplFd = HookSpec.field(Type.getType("Ljava/net/SocketImpl;"), "fd",
        Type.getType("Ljava/io/FileDescriptor;"));
    HookArgument socketImplLocalport = HookSpec.field(Type.getType("Ljava/net/SocketImpl;"),
        "localport", Type.INT_TYPE);
    nioSocketImplSpec.addMethodHookSpec(
        "void connect(java.net.SocketAddress,int)",
        "void afterConnect(java.io.FileDescriptor,java.net.SocketAddress,int)",
        socketImplFd, ARG0, socketImplLocalport);
    nioSocketImplSpec.addMethodHookSpec(
        "void accept(java.net.SocketImpl)",
        "void afterAccept(java.net.SocketImpl,int)",
        ARG0, socketImplLocalport);
    nioSocketImplSpec.addMethodHookSpec(
        "int tryRead(java.io.FileDescriptor,byte[],int,int)",
        "void afterTryRead(int,java.io.FileDescriptor,byte[],int)",
        ARG0, ARG1, ARG2);
    nioSocketImplSpec.addMethodHookSpec(
        "int tryWrite(java.io.FileDescriptor,byte[],int,int)",
        "void afterTryWrite(int,java.io.FileDescriptor,byte[],int)",
        ARG0, ARG1, ARG2);
    register(nioSocketImplSpec);

    // JDK 11
    ClassHookSpec apSocketImplSpec =
        new ClassHookSpec("java/net/AbstractPlainSocketImpl", SocketImplHook.class);
    apSocketImplSpec.addMethodHookSpec(
        "void connect(java.net.SocketAddress,int)",
        "void afterConnect(java.io.FileDescriptor,java.net.SocketAddress,int)",
        socketImplFd, ARG0, socketImplLocalport);
    apSocketImplSpec.addMethodHookSpec(
        "void accept(java.net.SocketImpl)",
        "void afterAccept(java.net.SocketImpl,int)",
        ARG0, socketImplLocalport);
    register(apSocketImplSpec);

    // JDK 11
    ClassHookSpec netSocketInputStreamSpec =
        new ClassHookSpec("java/net/SocketInputStream", NetSocketInputStreamHook.class);
    netSocketInputStreamSpec.addMethodHookSpec(
        "int socketRead(java.io.FileDescriptor,byte[],int,int,int)",
        "void afterSocketRead(int,java.io.FileDescriptor,byte[],int)",
        ARG0, ARG1, ARG2);
    register(netSocketInputStreamSpec);

    // JDK 11
    ClassHookSpec netSocketOutputStreamSpec = new ClassHookSpec(
        "java/net/SocketOutputStream", NetSocketOutputStreamHook.class);
    netSocketOutputStreamSpec.addMethodHookSpec(
        "void socketWrite(byte[],int,int)",
        "void afterSocketWrite(java.io.FileOutputStream,byte[],int,int)",
        THIS, ARG0, ARG1, ARG2);
    register(netSocketOutputStreamSpec);

    ClassHookSpec socketChannelImplSpec = new ClassHookSpec(
        "sun/nio/ch/SocketChannelImpl", SocketChannelImplHook.class);
    HookArgument socketChannelImplFd = HookSpec.field(
        Type.getType("Lsun/nio/ch/SocketChannelImpl;"), "fd",
        Type.getType("Ljava/io/FileDescriptor;"));
    socketChannelImplSpec.addMethodHookSpec(
        "boolean connect(java.net.SocketAddress)",
        "void afterConnect(boolean,java.nio.channels.SocketChannel,java.io.FileDescriptor)",
        THIS, socketChannelImplFd);
    register(socketChannelImplSpec);

    ClassHookSpec serverSocketChannelImplSpec = new ClassHookSpec(
        "sun/nio/ch/ServerSocketChannelImpl", SocketChannelImplHook.class);
    if (version > 11) {
      serverSocketChannelImplSpec.addMethodHookSpec(
          "java.nio.channels.SocketChannel finishAccept(java.io.FileDescriptor,java.net.SocketAddress)",
          "void afterFinishAccept(java.nio.channels.SocketChannel,java.io.FileDescriptor)",
          ARG0);
    } else {
      serverSocketChannelImplSpec.addMethodHookSpec(
          "java.nio.channels.SocketChannel accept()",
          "void afterAccept(java.nio.channels.SocketChannel)");
    }
    register(serverSocketChannelImplSpec);

    ClassHookSpec inflaterInputStreamSpec = new ClassHookSpec(
        "java/util/zip/InflaterInputStream", InflaterInputStreamHook.class);
    inflaterInputStreamSpec.addMethodHookSpec(
        "void <init>(java.io.InputStream,java.util.zip.Inflater,int)",
        "void afterInit(java.util.zip.InflaterInputStream,java.io.InputStream)",
        THIS, ARG0);
    inflaterInputStreamSpec.addMethodHookSpec("int read(byte[],int,int)",
        "void afterReadByteArrayOffset(int,java.util.zip.InflaterInputStream,byte[],int)",
        THIS, ARG0, ARG1);
    inflaterInputStreamSpec.addMethodHookSpec("int read()",
        "void afterRead1(int,java.util.zip.InflaterInputStream,be.coekaerts.wouter.flowtracker.tracker.Invocation)",
        THIS, INVOCATION);
    register(inflaterInputStreamSpec);

    ClassHookSpec zipFileSpec = new ClassHookSpec(
        "java/util/zip/ZipFile", ZipFileHook.class);
    zipFileSpec.addMethodHookSpec("java.io.InputStream getInputStream(java.util.zip.ZipEntry)",
        "void afterGetInputStream(java.io.InputStream,java.util.zip.ZipFile,java.util.zip.ZipEntry)",
        THIS, ARG0);
    register(zipFileSpec);
  }

  private void register(ClassHookSpec spec) {
    specs.put(spec.getTargetClass().getInternalName(), spec);
  }

  private ClassHookSpec getSpec(String className) {
    if (className.endsWith("URLConnection")) {
      return urlConnectionHook(className);
    }
    return specs.get(className);
  }

  private ClassHookSpec urlConnectionHook(String urlConnectionSubclass) {
    ClassHookSpec spec = new ClassHookSpec(
        urlConnectionSubclass.replace('.', '/'), URLConnectionHook.class);
    spec.addMethodHookSpec("java.io.InputStream getInputStream()",
        "void afterGetInputStream(java.io.InputStream,java.net.URLConnection)", THIS);
    return spec;
  }

  // not used yet, see CharsetEncoderTest
//  private ClassHookSpec charsetEncoderSpec() {
//    return new ClassHookSpec("java/nio/charset/CharsetEncoder", CharsetEncoderHook.class)
//        .addMethodHookSpec(
//            "java.nio.charset.CoderResult encode(java.nio.CharBuffer,java.nio.ByteBuffer,boolean)",
//            "void afterEncode(int,int,java.nio.CharBuffer,java.nio.ByteBuffer)", ...);
//  }

  @Override
  public ClassVisitor transform(String className, ClassVisitor cv) {
    ClassHookSpec spec = getSpec(className);
    return spec == null ? cv : spec.transform(className, cv);
  }
}
