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
import java.io.FileDescriptor;

/**
 * Hooks for java.net.SocketInputStream, for JDK 11.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class NetSocketInputStreamHook {
  @Hook(target = "java.net.SocketInputStream",
      condition = "version < 17",
      method = "int socketRead(java.io.FileDescriptor,byte[],int,int,int)")
  public static void afterSocketRead(@Arg("RETURN") int read, @Arg("ARG0") FileDescriptor fd,
      @Arg("ARG1") byte[] buf, @Arg("ARG2") int offset) {
    FileInputStreamHook.afterReadByteArrayOffset(read, fd, buf, offset);
  }
}
