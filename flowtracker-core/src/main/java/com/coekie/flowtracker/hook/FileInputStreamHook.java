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
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import java.io.File;
import java.io.FileDescriptor;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class FileInputStreamHook {

  @Hook(target = "java.io.FileInputStream",
      method = "void <init>(java.io.File)")
  public static void afterInit(@Arg("FileInputStream_fd") FileDescriptor fd,
      @Arg("ARG0") File file) {
    if (context().isActive() && !ClassLoaderHook.shouldHideFileReading(file.getPath())) {
      FileDescriptorTrackerRepository.createTracker(fd, true, false,
          TrackerTree.fileNode(file.getAbsolutePath()));
    }
  }

  // note: there is no hook for the constructor that takes a FileDescriptor

  @Hook(target = "java.io.FileInputStream",
      method = "int read()")
  public static void afterRead1(@Arg("RETURN") int result,
      @Arg("FileInputStream_fd") FileDescriptor fd, @Arg("INVOCATION") Invocation invocation) {
    if (result > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        Invocation.returning(invocation, TrackerPoint.of(tracker, tracker.getLength()));
        tracker.append((byte) result);
      }
    }
  }

  @Hook(target = "java.io.FileInputStream",
      method = "int read(byte[])")
  public static void afterReadByteArray(@Arg("RETURN") int read,
      @Arg("FileInputStream_fd") FileDescriptor fd, @Arg("ARG0") byte[] buf) {
    if (read > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, 0, read, tracker, tracker.getLength());
        tracker.append(buf, 0, read);
      }
    }
  }

  @Hook(target = "java.io.FileInputStream",
      method = "int read(byte[],int,int)")
  public static void afterReadByteArrayOffset(@Arg("RETURN") int read,
      @Arg("FileInputStream_fd") FileDescriptor fd, @Arg("ARG0") byte[] buf,
      @Arg("ARG1") int offset) {
    if (read > 0) {
      ByteOriginTracker tracker = FileDescriptorTrackerRepository.getReadTracker(fd);
      if (tracker != null) {
        TrackerUpdater.setSourceTracker(buf, offset, read, tracker, tracker.getLength());
        tracker.append(buf, offset, read);
      }
    }
  }
}
