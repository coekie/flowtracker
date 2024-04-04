package be.coekaerts.wouter.flowtracker.weaver;

import static be.coekaerts.wouter.flowtracker.weaver.HookArgs.FileChannelImpl_fd;
import static be.coekaerts.wouter.flowtracker.weaver.HookArgs.FileInputStream_fd;
import static be.coekaerts.wouter.flowtracker.weaver.HookArgs.FileOutputStream_fd;
import static be.coekaerts.wouter.flowtracker.weaver.HookArgs.SocketChannelImpl_fd;
import static be.coekaerts.wouter.flowtracker.weaver.HookArgs.SocketImpl_fd;
import static be.coekaerts.wouter.flowtracker.weaver.HookArgs.SocketImpl_localport;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG0;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG1;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG2;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.ARG3;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.INVOCATION;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.RETURN;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.THIS;

import javax.annotation.processing.Generated;

@Generated("be.coekaerts.wouter.flowtracker.generator.HookSpecGenerator")
class GeneratedHookSpecs {
  static HookSpecTransformer createTransformer() {
    int version = Runtime.version().feature();
    HookSpecTransformer t = new HookSpecTransformer();
    if (version < 17) t.register("java/nio/DirectByteBuffer",
      "get",
      "([BII)Ljava/nio/ByteBuffer;",
      "be/coekaerts/wouter/flowtracker/hook/ByteBufferHook",
      "afterDirectBufferGet",
      "([BII)V",
      ARG0, ARG1, ARG2);
    if (version > 11) t.register("java/nio/ByteBuffer",
      "putBuffer",
      "(ILjava/nio/ByteBuffer;II)V",
      "be/coekaerts/wouter/flowtracker/hook/ByteBufferHook",
      "afterPutBuffer",
      "(Ljava/nio/ByteBuffer;ILjava/nio/ByteBuffer;II)V",
      THIS, ARG0, ARG1, ARG2, ARG3);
    t.register("sun/nio/ch/FileChannelImpl",
      "<init>",
      "(Ljava/io/FileDescriptor;Ljava/lang/String;ZZZLjava/lang/Object;)V",
      "be/coekaerts/wouter/flowtracker/hook/FileChannelImplHook",
      "afterInit",
      "(Ljava/io/FileDescriptor;Ljava/lang/String;ZZ)V",
      FileChannelImpl_fd, ARG1, ARG2, ARG3);
    t.register("sun/nio/ch/FileChannelImpl",
      "<init>",
      "(Ljava/io/FileDescriptor;Ljava/lang/String;ZZZLjava/io/Closeable;)V",
      "be/coekaerts/wouter/flowtracker/hook/FileChannelImplHook",
      "afterInit",
      "(Ljava/io/FileDescriptor;Ljava/lang/String;ZZ)V",
      FileChannelImpl_fd, ARG1, ARG2, ARG3);
    t.register("java/io/FileInputStream",
      "<init>",
      "(Ljava/io/File;)V",
      "be/coekaerts/wouter/flowtracker/hook/FileInputStreamHook",
      "afterInit",
      "(Ljava/io/FileDescriptor;Ljava/io/File;)V",
      FileInputStream_fd, ARG0);
    t.register("java/io/FileInputStream",
      "read",
      "()I",
      "be/coekaerts/wouter/flowtracker/hook/FileInputStreamHook",
      "afterRead1",
      "(ILjava/io/FileDescriptor;Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;)V",
      RETURN, FileInputStream_fd, INVOCATION);
    t.register("java/io/FileInputStream",
      "read",
      "([B)I",
      "be/coekaerts/wouter/flowtracker/hook/FileInputStreamHook",
      "afterReadByteArray",
      "(ILjava/io/FileDescriptor;[B)V",
      RETURN, FileInputStream_fd, ARG0);
    t.register("java/io/FileInputStream",
      "read",
      "([BII)I",
      "be/coekaerts/wouter/flowtracker/hook/FileInputStreamHook",
      "afterReadByteArrayOffset",
      "(ILjava/io/FileDescriptor;[BI)V",
      RETURN, FileInputStream_fd, ARG0, ARG1);
    t.register("java/io/FileOutputStream",
      "<init>",
      "(Ljava/io/File;Z)V",
      "be/coekaerts/wouter/flowtracker/hook/FileOutputStreamHook",
      "afterInit",
      "(Ljava/io/FileDescriptor;Ljava/io/File;)V",
      FileOutputStream_fd, ARG0);
    t.register("java/io/FileOutputStream",
      "write",
      "(I)V",
      "be/coekaerts/wouter/flowtracker/hook/FileOutputStreamHook",
      "afterWrite1",
      "(Ljava/io/FileDescriptor;ILbe/coekaerts/wouter/flowtracker/tracker/Invocation;)V",
      FileOutputStream_fd, ARG0, INVOCATION);
    t.register("java/io/FileOutputStream",
      "write",
      "([B)V",
      "be/coekaerts/wouter/flowtracker/hook/FileOutputStreamHook",
      "afterWriteByteArray",
      "(Ljava/io/FileDescriptor;[B)V",
      FileOutputStream_fd, ARG0);
    t.register("java/io/FileOutputStream",
      "write",
      "([BII)V",
      "be/coekaerts/wouter/flowtracker/hook/FileOutputStreamHook",
      "afterWriteByteArrayOffset",
      "(Ljava/io/FileDescriptor;[BII)V",
      FileOutputStream_fd, ARG0, ARG1, ARG2);
    if (version >= 17) t.register("sun/nio/ch/IOUtil",
      "read",
      "(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;JZZILsun/nio/ch/NativeDispatcher;)I",
      "be/coekaerts/wouter/flowtracker/hook/IOUtilHook",
      "afterReadByteBuffer",
      "(ILjava/io/FileDescriptor;Ljava/nio/ByteBuffer;J)V",
      RETURN, ARG0, ARG1, ARG2);
    if (version < 17) t.register("sun/nio/ch/IOUtil",
      "read",
      "(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;JZILsun/nio/ch/NativeDispatcher;)I",
      "be/coekaerts/wouter/flowtracker/hook/IOUtilHook",
      "afterReadByteBuffer",
      "(ILjava/io/FileDescriptor;Ljava/nio/ByteBuffer;J)V",
      RETURN, ARG0, ARG1, ARG2);
    if (version >= 17) t.register("sun/nio/ch/IOUtil",
      "write",
      "(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;JZZILsun/nio/ch/NativeDispatcher;)I",
      "be/coekaerts/wouter/flowtracker/hook/IOUtilHook",
      "afterWriteByteBuffer",
      "(ILjava/io/FileDescriptor;Ljava/nio/ByteBuffer;J)V",
      RETURN, ARG0, ARG1, ARG2);
    if (version < 17) t.register("sun/nio/ch/IOUtil",
      "write",
      "(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;JZILsun/nio/ch/NativeDispatcher;)I",
      "be/coekaerts/wouter/flowtracker/hook/IOUtilHook",
      "afterWriteByteBuffer",
      "(ILjava/io/FileDescriptor;Ljava/nio/ByteBuffer;J)V",
      RETURN, ARG0, ARG1, ARG2);
    t.register("java/util/zip/InflaterInputStream",
      "<init>",
      "(Ljava/io/InputStream;Ljava/util/zip/Inflater;I)V",
      "be/coekaerts/wouter/flowtracker/hook/InflaterInputStreamHook",
      "afterInit",
      "(Ljava/util/zip/InflaterInputStream;Ljava/io/InputStream;)V",
      THIS, ARG0);
    t.register("java/util/zip/InflaterInputStream",
      "read",
      "()I",
      "be/coekaerts/wouter/flowtracker/hook/InflaterInputStreamHook",
      "afterRead1",
      "(ILjava/util/zip/InflaterInputStream;Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;)V",
      RETURN, THIS, INVOCATION);
    t.register("java/util/zip/InflaterInputStream",
      "read",
      "([BII)I",
      "be/coekaerts/wouter/flowtracker/hook/InflaterInputStreamHook",
      "afterReadByteArrayOffset",
      "(ILjava/util/zip/InflaterInputStream;[BI)V",
      RETURN, THIS, ARG0, ARG1);
    if (version < 17) t.register("java/net/SocketInputStream",
      "socketRead",
      "(Ljava/io/FileDescriptor;[BIII)I",
      "be/coekaerts/wouter/flowtracker/hook/NetSocketInputStreamHook",
      "afterSocketRead",
      "(ILjava/io/FileDescriptor;[BI)V",
      RETURN, ARG0, ARG1, ARG2);
    if (version < 17) t.register("java/net/SocketOutputStream",
      "socketWrite",
      "([BII)V",
      "be/coekaerts/wouter/flowtracker/hook/NetSocketOutputStreamHook",
      "afterSocketWrite",
      "(Ljava/io/FileOutputStream;[BII)V",
      THIS, ARG0, ARG1, ARG2);
    t.register("java/io/OutputStreamWriter",
      "<init>",
      "(Ljava/io/OutputStream;)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterInit",
      "(Ljava/io/OutputStreamWriter;Ljava/io/OutputStream;)V",
      THIS, ARG0);
    t.register("java/io/OutputStreamWriter",
      "<init>",
      "(Ljava/io/OutputStream;Ljava/lang/String;)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterInit",
      "(Ljava/io/OutputStreamWriter;Ljava/io/OutputStream;)V",
      THIS, ARG0);
    t.register("java/io/OutputStreamWriter",
      "<init>",
      "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterInit",
      "(Ljava/io/OutputStreamWriter;Ljava/io/OutputStream;)V",
      THIS, ARG0);
    t.register("java/io/OutputStreamWriter",
      "<init>",
      "(Ljava/io/OutputStream;Ljava/nio/charset/CharsetEncoder;)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterInit",
      "(Ljava/io/OutputStreamWriter;Ljava/io/OutputStream;)V",
      THIS, ARG0);
    t.register("java/io/OutputStreamWriter",
      "write",
      "(I)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterWrite1",
      "(Ljava/io/OutputStreamWriter;ILbe/coekaerts/wouter/flowtracker/tracker/Invocation;)V",
      THIS, ARG0, INVOCATION);
    t.register("java/io/OutputStreamWriter",
      "write",
      "([CII)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterWriteCharArrayOffset",
      "(Ljava/io/OutputStreamWriter;[CII)V",
      THIS, ARG0, ARG1, ARG2);
    t.register("java/io/OutputStreamWriter",
      "write",
      "(Ljava/lang/String;II)V",
      "be/coekaerts/wouter/flowtracker/hook/OutputStreamWriterHook",
      "afterWriteStringOffset",
      "(Ljava/io/OutputStreamWriter;Ljava/lang/String;II)V",
      THIS, ARG0, ARG1, ARG2);
    if (version <= 11) t.register("sun/nio/ch/ServerSocketChannelImpl",
      "accept",
      "()Ljava/nio/channels/SocketChannel;",
      "be/coekaerts/wouter/flowtracker/hook/SocketChannelImplHook",
      "afterAccept",
      "(Ljava/nio/channels/SocketChannel;)V",
      RETURN);
    t.register("sun/nio/ch/SocketChannelImpl",
      "connect",
      "(Ljava/net/SocketAddress;)Z",
      "be/coekaerts/wouter/flowtracker/hook/SocketChannelImplHook",
      "afterConnect",
      "(ZLjava/nio/channels/SocketChannel;Ljava/io/FileDescriptor;)V",
      RETURN, THIS, SocketChannelImpl_fd);
    if (version > 11) t.register("sun/nio/ch/ServerSocketChannelImpl",
      "finishAccept",
      "(Ljava/io/FileDescriptor;Ljava/net/SocketAddress;)Ljava/nio/channels/SocketChannel;",
      "be/coekaerts/wouter/flowtracker/hook/SocketChannelImplHook",
      "afterFinishAccept",
      "(Ljava/nio/channels/SocketChannel;Ljava/io/FileDescriptor;)V",
      RETURN, ARG0);
    if (version >= 17) t.register("sun/nio/ch/NioSocketImpl",
      "accept",
      "(Ljava/net/SocketImpl;)V",
      "be/coekaerts/wouter/flowtracker/hook/SocketImplHook",
      "afterAccept",
      "(Ljava/net/SocketImpl;I)V",
      ARG0, SocketImpl_localport);
    if (version < 17) t.register("java/net/AbstractPlainSocketImpl",
      "accept",
      "(Ljava/net/SocketImpl;)V",
      "be/coekaerts/wouter/flowtracker/hook/SocketImplHook",
      "afterAccept",
      "(Ljava/net/SocketImpl;I)V",
      ARG0, SocketImpl_localport);
    if (version >= 17) t.register("sun/nio/ch/NioSocketImpl",
      "connect",
      "(Ljava/net/SocketAddress;I)V",
      "be/coekaerts/wouter/flowtracker/hook/SocketImplHook",
      "afterConnect",
      "(Ljava/io/FileDescriptor;Ljava/net/SocketAddress;I)V",
      SocketImpl_fd, ARG0, SocketImpl_localport);
    if (version < 17) t.register("java/net/AbstractPlainSocketImpl",
      "connect",
      "(Ljava/net/SocketAddress;I)V",
      "be/coekaerts/wouter/flowtracker/hook/SocketImplHook",
      "afterConnect",
      "(Ljava/io/FileDescriptor;Ljava/net/SocketAddress;I)V",
      SocketImpl_fd, ARG0, SocketImpl_localport);
    if (version >= 17) t.register("sun/nio/ch/NioSocketImpl",
      "tryRead",
      "(Ljava/io/FileDescriptor;[BII)I",
      "be/coekaerts/wouter/flowtracker/hook/SocketImplHook",
      "afterTryRead",
      "(ILjava/io/FileDescriptor;[BI)V",
      RETURN, ARG0, ARG1, ARG2);
    if (version >= 17) t.register("sun/nio/ch/NioSocketImpl",
      "tryWrite",
      "(Ljava/io/FileDescriptor;[BII)I",
      "be/coekaerts/wouter/flowtracker/hook/SocketImplHook",
      "afterTryWrite",
      "(ILjava/io/FileDescriptor;[BI)V",
      RETURN, ARG0, ARG1, ARG2);
    t.register("java/util/zip/ZipFile",
      "getInputStream",
      "(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;",
      "be/coekaerts/wouter/flowtracker/hook/ZipFileHook",
      "afterGetInputStream",
      "(Ljava/io/InputStream;Ljava/util/zip/ZipFile;Ljava/util/zip/ZipEntry;)V",
      RETURN, THIS, ARG0);
    t.register("org/springframework/boot/loader/jar/NestedJarFile",
      "getInputStream",
      "(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;",
      "be/coekaerts/wouter/flowtracker/hook/ZipFileHook",
      "afterGetInputStream",
      "(Ljava/io/InputStream;Ljava/util/zip/ZipFile;Ljava/util/zip/ZipEntry;)V",
      RETURN, THIS, ARG0);
    return t;
  }
}