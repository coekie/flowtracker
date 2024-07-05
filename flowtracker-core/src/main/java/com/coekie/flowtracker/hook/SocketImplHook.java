package com.coekie.flowtracker.hook;

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

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImpl;

/**
 * Hooks for AbstractPlainSocketImpl (jdk 11) and NioSocketImpl (jdk 17+)
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SocketImplHook {
  private static final VarHandle fdHandle =
      Reflection.varHandle(SocketImpl.class, "fd", FileDescriptor.class);
  private static final VarHandle addressHandle =
      Reflection.varHandle(SocketImpl.class, "address", InetAddress.class);
  private static final VarHandle portHandle =
      Reflection.varHandle(SocketImpl.class, "port", int.class);

  private static final VarHandle socketImplHandle =
      Reflection.varHandle(Socket.class, "impl", SocketImpl.class);
  private static final Class<?> delegatingSocketImplClass = delegatingSocketImplClass();
  private static final MethodHandle delegateGetter = delegatingSocketImplClass == null
      ? null
      : Reflection.getter(delegatingSocketImplClass, "delegate", SocketImpl.class);

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "void connect(java.net.SocketAddress,int)")
  @Hook(target = "java.net.AbstractPlainSocketImpl",
      condition = "version < 17",
      method = "void connect(java.net.SocketAddress,int)")
  public static void afterConnect(@Arg("SocketImpl_fd") FileDescriptor fd,
      @Arg("ARG0") SocketAddress remote, @Arg("SocketImpl_localport") int localport) {
    if (context().isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          clientSocketNode(remote, localport));
    }
  }

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "void accept(java.net.SocketImpl)")
  @Hook(target = "java.net.AbstractPlainSocketImpl",
      condition = "version < 17",
      method = "void accept(java.net.SocketImpl)")
  public static void afterAccept(@Arg("ARG0") SocketImpl si,
      @Arg("SocketImpl_localport") int localport) {
    if (context().isActive()) {
      FileDescriptor fd = fd(si);
      InetAddress address = address(si);
      int port = port(si);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          serverSocketNode(Integer.toString(localport), address + ":" + port));
    }
  }

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "int tryRead(java.io.FileDescriptor,byte[],int,int)")
  public static void afterTryRead(@Arg("RETURN") int read, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") byte[] buf, @Arg("ARG2") int offset) {
    FileInputStreamHook.afterReadByteArrayOffset(read, fd, buf, offset);
  }

  @Hook(target = "sun.nio.ch.NioSocketImpl",
      condition = "version >= 17",
      method = "int tryWrite(java.io.FileDescriptor,byte[],int,int)")
  public static void afterTryWrite(@Arg("RETURN") int written, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") byte[] buf, @Arg("ARG2") int off) {
    FileOutputStreamHook.afterWriteByteArrayOffset(fd, buf, off, written);
  }

  static Node serverSocketNode(SocketAddress localAddress, SocketAddress remote) {
    if (localAddress instanceof InetSocketAddress) {
      return serverSocketNode(Integer.toString(((InetSocketAddress) localAddress).getPort()),
          remote.toString());
    } else { // e.g. unix domain sockets? not really tested/supported yet
      return serverSocketNode(localAddress.toString(), remote.toString());
    }
  }

  static Node serverSocketNode(String localPort, String remote) {
    return TrackerTree.node("Server socket").optionalNode(localPort).node(remote);
  }

  static TrackerTree.Node clientSocketNode(SocketAddress remote, int localPort) {
    return clientSocketNodeWithoutPort(remote)
        .optionalNode("From " + localPort);
  }

  static TrackerTree.Node clientSocketNodeWithoutPort(SocketAddress remote) {
    return TrackerTree.node("Client socket").node(remote.toString());
  }

  private static FileDescriptor fd(SocketImpl socketImpl) {
    return (FileDescriptor) fdHandle.get(socketImpl);
  }

  private static InetAddress address(SocketImpl socketImpl) {
    return (InetAddress) addressHandle.get(socketImpl);
  }

  private static int port(SocketImpl socketImpl) {
    return (int) portHandle.get(socketImpl);
  }

  /**
   * Find the {@link java.io.FileDescriptor} backing a Socket, poking through some layers of
   * indirection.
   */
  public static FileDescriptor getSocketFd(Socket socket) {
    SocketImpl impl = (SocketImpl) socketImplHandle.get(socket);

    // find the fd either in the SocketImpl, or in one that it (SocksSocketImpl /
    // DelegatingSocketImpl) delegates to.
    // if there's a delegation going on depends both on if it's a client or server socket, and the
    // JDK version
    while (true) {
      FileDescriptor fd = fd(impl);
      if (fd != null) {
        return fd;
      }
      if (delegatingSocketImplClass != null && delegatingSocketImplClass.isInstance(impl)) {
        try {
          impl = (SocketImpl) delegateGetter.invoke(impl);
        } catch (Throwable e) {
          throw new Error(e);
        }
      } else {
        return null;
      }
    }
  }

  private static Class<?> delegatingSocketImplClass() {
    try {
      return Class.forName("java.net.DelegatingSocketImpl", false, null);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}