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

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SocketChannelImplHook {
  private static final Field remoteAddressField =
      Reflection.getDeclaredField("sun.nio.ch.SocketChannelImpl", "remoteAddress");
  private static final Field localAddressField =
      Reflection.getDeclaredField("sun.nio.ch.SocketChannelImpl", "localAddress");
  private static final Field fdField =
      Reflection.getDeclaredField("sun.nio.ch.SocketChannelImpl", "fd");

  @Hook(target = "sun.nio.ch.SocketChannelImpl",
      method = "boolean connect(java.net.SocketAddress)")
  public static void afterConnect(@Arg("RETURN") boolean result, @Arg("THIS") SocketChannel channel,
      @Arg("SocketChannelImpl_fd") FileDescriptor fd) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.clientSocketNode(remoteAddress));
    }
  }

  @Hook(target = "sun.nio.ch.ServerSocketChannelImpl", // not SocketChannelImpl
      condition = "version > 11",
      method = "java.nio.channels.SocketChannel finishAccept(java.io.FileDescriptor,java.net.SocketAddress)")
  public static void afterFinishAccept(@Arg("RETURN") SocketChannel channel,
      @Arg("ARG0") FileDescriptor fd) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.serverSocketNode(localAddress, remoteAddress));
    }
  }

  @Hook(target = "sun.nio.ch.ServerSocketChannelImpl", // not SocketChannelImpl
      condition = "version <= 11",
      method = "java.nio.channels.SocketChannel accept()")
  public static void afterAccept(@Arg("RETURN") SocketChannel channel) {
    if (Trackers.isActive()) {
      SocketAddress remoteAddress =
          (SocketAddress) Reflection.getFieldValue(channel, remoteAddressField);
      SocketAddress localAddress =
          (SocketAddress) Reflection.getFieldValue(channel, localAddressField);
      FileDescriptor fd =
          (FileDescriptor) Reflection.getFieldValue(channel, fdField);
      FileDescriptorTrackerRepository.createTracker(fd, true, true,
          SocketImplHook.serverSocketNode(localAddress, remoteAddress));
    }
  }
}
