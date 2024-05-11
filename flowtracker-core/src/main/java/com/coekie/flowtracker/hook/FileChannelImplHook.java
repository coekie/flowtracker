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
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.Trackers;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileChannelImplHook {
  // for JDK<=17
  @Hook(target = "sun.nio.ch.FileChannelImpl",
      method = "void <init>(java.io.FileDescriptor,java.lang.String,boolean,boolean,boolean,java.lang.Object)")
  // for JDK>=21
  @Hook(target = "sun.nio.ch.FileChannelImpl",
      method = "void <init>(java.io.FileDescriptor,java.lang.String,boolean,boolean,boolean,java.io.Closeable)")
  public static void afterInit(@Arg("FileChannelImpl_fd") FileDescriptor fd,
      @Arg("ARG1") String path, @Arg("ARG2") boolean readable, @Arg("ARG3") boolean writeable) {
    if (Trackers.isActive()) {
      if (!FileDescriptorTrackerRepository.hasTracker(fd)) {
        FileDescriptorTrackerRepository.createTracker(fd, readable, writeable,
            TrackerTree.fileNode(path));
      }
    }
  }
}
