package com.coekie.flowtracker.weaver;

import com.coekie.flowtracker.weaver.HookSpec.HookArgument;
import org.objectweb.asm.Type;

interface HookArgs {
  HookArgument FileInputStream_fd = HookSpec.field(Type.getType("Ljava/io/FileInputStream;"),
      "fd", Type.getType("Ljava/io/FileDescriptor;"));
  HookArgument FileOutputStream_fd = HookSpec.field(Type.getType("Ljava/io/FileOutputStream;"),
      "fd", Type.getType("Ljava/io/FileDescriptor;"));
  HookArgument FileChannelImpl_fd = HookSpec.field(Type.getType("Lsun/nio/ch/FileChannelImpl;"),
      "fd", Type.getType("Ljava/io/FileDescriptor;"));
  HookArgument SocketImpl_fd = HookSpec.field(Type.getType("Ljava/net/SocketImpl;"), "fd",
      Type.getType("Ljava/io/FileDescriptor;"));
  HookArgument SocketImpl_localport = HookSpec.field(Type.getType("Ljava/net/SocketImpl;"),
      "localport", Type.INT_TYPE);
  HookArgument SocketChannelImpl_fd = HookSpec.field(
      Type.getType("Lsun/nio/ch/SocketChannelImpl;"), "fd",
      Type.getType("Ljava/io/FileDescriptor;"));
}
