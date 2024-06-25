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
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileOutputStreamHook {

  @Hook(target = "java.io.FileOutputStream",
      method = "void <init>(java.io.File,boolean)")
  public static void afterInit(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") File file) {
    if (context().isActive()) {
      FileDescriptorTrackerRepository.createTracker(fd, false, true,
          TrackerTree.fileNode(file.getAbsolutePath()));
    }
  }

  @Hook(target = "java.io.FileOutputStream",
      method = "void write(int)")
  public static void afterWrite1(@Arg("FileOutputStream_fd") FileDescriptor fd, @Arg("ARG0") int c,
      @Arg("INVOCATION") Invocation invocation) {
    ByteSinkTracker tracker = FileDescriptorTrackerRepository.getWriteTracker(context(), fd);
    if (tracker != null) {
      TrackerPoint sourcePoint = Invocation.getArgPoint(invocation, 0);
      if (sourcePoint != null) {
        tracker.setSource(tracker.getLength(), 1, sourcePoint);
      }
      tracker.append((byte) c);
    }
  }

  @Hook(target = "java.io.FileOutputStream",
      method = "void write(byte[])")
  public static void afterWriteByteArray(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") byte[] buf) {
    afterWriteByteArrayOffset(fd, buf, 0, buf.length);
  }

  @Hook(target = "java.io.FileOutputStream",
      method = "void write(byte[],int,int)")
  public static void afterWriteByteArrayOffset(@Arg("FileOutputStream_fd") FileDescriptor fd,
      @Arg("ARG0") byte[] buf, @Arg("ARG1") int off, @Arg("ARG2") int len) {
    Context context = context();
    ByteSinkTracker tracker = FileDescriptorTrackerRepository.getWriteTracker(context, fd);
    if (tracker != null) {
      TrackerUpdater.appendBytes(context, tracker, buf, off, len);
    }
  }
}
