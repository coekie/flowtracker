package com.coekie.flowtracker.hook;

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
    TrackerUpdater.setSourceTrackerPoint(array, offset, 2, Invocation.getArgPoint(invocation, 2));
  }

  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setShort(byte[], int, short)")
  public static void afterSetShort(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(array, offset, 2, Invocation.getArgPoint(invocation, 2));
  }

  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setUnsignedShort(byte[], int, int)")
  public static void afterSetUnsignedShort(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(array, offset, 2, Invocation.getArgPoint(invocation, 2));
  }

  @Hook(target = "jdk.internal.util.ByteArray",
      condition = "version >= 21",
      method = "void setInt(byte[], int, int)")
  public static void afterSetInt(@Arg("ARG0") byte[] array, @Arg("ARG1") int offset,
      @Arg("INVOCATION") Invocation invocation) {
    TrackerUpdater.setSourceTrackerPoint(array, offset, 4, Invocation.getArgPoint(invocation, 2));
  }
}
