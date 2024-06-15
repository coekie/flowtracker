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
import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SocketChannelImplHook {
  public static final Class<?> CLASS = Reflection.clazz("sun.nio.ch.SocketChannelImpl");
  private static final MethodHandle remoteAddressHandle =
      Reflection.getter(CLASS, "remoteAddress",
          Runtime.version().feature() > 11 ? SocketAddress.class : InetSocketAddress.class);
  private static final MethodHandle localAddressHandle =
      Reflection.getter(CLASS, "localAddress",
          Runtime.version().feature() > 11 ? SocketAddress.class : InetSocketAddress.class);
  private static final MethodHandle fdHandle = Reflection.getter(CLASS, "fd", FileDescriptor.class);

  @Hook(target = "sun.nio.ch.SocketChannelImpl",
      method = "boolean connect(java.net.SocketAddress)")
  public static void afterConnect(@Arg("RETURN") boolean result, @Arg("THIS") SocketChannel channel,
      @Arg("SocketChannelImpl_fd") FileDescriptor fd) {
    if (context().isActive()) {
      SocketAddress remoteAddress = remoteAddress(channel);
      SocketAddress localAddress = localAddress(channel);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.clientSocketNode(remoteAddress));
    }
  }

  @Hook(target = "sun.nio.ch.ServerSocketChannelImpl", // not SocketChannelImpl
      condition = "version > 11",
      method = "java.nio.channels.SocketChannel finishAccept(java.io.FileDescriptor,java.net.SocketAddress)")
  public static void afterFinishAccept(@Arg("RETURN") SocketChannel channel,
      @Arg("ARG0") FileDescriptor fd) {
    if (context().isActive()) {
      SocketAddress remoteAddress = remoteAddress(channel);
      SocketAddress localAddress = localAddress(channel);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.serverSocketNode(localAddress, remoteAddress));
    }
  }

  @Hook(target = "sun.nio.ch.ServerSocketChannelImpl", // not SocketChannelImpl
      condition = "version <= 11",
      method = "java.nio.channels.SocketChannel accept()")
  public static void afterAccept(@Arg("RETURN") SocketChannel channel) {
    if (context().isActive()) {
      SocketAddress remoteAddress = remoteAddress(channel);
      SocketAddress localAddress = localAddress(channel);
      FileDescriptor fd = fd(channel);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.serverSocketNode(localAddress, remoteAddress));
    }
  }

  private static SocketAddress remoteAddress(SocketChannel channel) {
    try {
      return (SocketAddress) remoteAddressHandle.invoke(channel);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  private static SocketAddress localAddress(SocketChannel channel) {
    try {
      return (SocketAddress) localAddressHandle.invoke(channel);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  private static FileDescriptor fd(SocketChannel channel) {
    try {
      return (FileDescriptor) fdHandle.invoke(channel);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }
}
