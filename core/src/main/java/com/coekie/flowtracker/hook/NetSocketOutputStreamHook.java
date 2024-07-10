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
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Hooks for java.net.SocketOutputStream, for JDK 11.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class NetSocketOutputStreamHook {
  @Hook(target = "java.net.SocketOutputStream",
      condition = "version < 17",
      method = "void socketWrite(byte[],int,int)")
  public static void afterSocketWrite(@Arg("THIS") FileOutputStream os, @Arg("ARG0") byte[] buf,
      @Arg("ARG1") int off, @Arg("ARG2") int len)
      throws IOException {
    FileOutputStreamHook.afterWriteByteArrayOffset(os.getFD(), buf, off, len);
  }
}
