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
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerUpdater;

/** Hook methods for `jdk.internal.util.ByteArray` */
public class ByteArrayHook {
  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setChar(byte[], int, char)")
  public static void afterSetChar(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(context(),
        array, offset, 2, Invocation.getArgPoint(invocation, 2));
  }

  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setShort(byte[], int, short)")
  public static void afterSetShort(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(context(),
        array, offset, 2, Invocation.getArgPoint(invocation, 2));
  }

  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setUnsignedShort(byte[], int, int)")
  public static void afterSetUnsignedShort(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(context(),
        array, offset, 2, Invocation.getArgPoint(invocation, 2));
  }

  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setInt(byte[], int, int)")
  public static void afterSetInt(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(context(),
        array, offset, 4, Invocation.getArgPoint(invocation, 2));
  }
}
