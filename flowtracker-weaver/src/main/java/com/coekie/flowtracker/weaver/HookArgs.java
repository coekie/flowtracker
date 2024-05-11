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
