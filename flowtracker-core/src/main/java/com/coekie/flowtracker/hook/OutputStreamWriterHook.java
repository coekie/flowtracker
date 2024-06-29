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
import com.coekie.flowtracker.tracker.CharSinkTracker;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.util.Config;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.invoke.VarHandle;

/**
 * Hook methods called by instrumented code for OutputStreamWriter.
 *
 * <p>This is partially redundant with tracking the content of FileOutputStream.
 * This is not enabled by default. This comes from a time before we had tracking on
 * FileOutputStream, before that could be tracked automatically through OutputStreamWriter.
 * In theory, we could still use this to track writing to other OutputStreams for which we don't
 * have tracking. In practice, I haven't seen it be useful, and should probably be removed.
 */
@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class OutputStreamWriterHook {
  private static final String TRACK_OUTPUT_STREAM_WRITER = "trackOutputStreamWriter";
  private static final String enabledCondition =
      "config.getBoolean(\"" + TRACK_OUTPUT_STREAM_WRITER + "\", false)";

  private static final VarHandle filterOutputStream_out =
      Reflection.varHandle(FilterOutputStream.class, "out", OutputStream.class);

  static boolean enabled(Config config) {
    return config.getBoolean(TRACK_OUTPUT_STREAM_WRITER, false);
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream)",
      condition = enabledCondition)
  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream,java.lang.String)",
      condition = "config.getBoolean(\"trackOutputStreamWriter\", false)")
  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream,java.nio.charset.Charset)",
      condition = enabledCondition)
  @Hook(target = "java.io.OutputStreamWriter",
      method = "void <init>(java.io.OutputStream,java.nio.charset.CharsetEncoder)",
      condition = enabledCondition)
  public static void afterInit(@Arg("THIS") OutputStreamWriter target,
      @Arg("ARG0") OutputStream stream) {
    Context context = context();
    if (context.isActive()) {
      createOutputStreamWriterTracker(context, target, stream);
    }
  }

  static void createOutputStreamWriterTracker(Context context, OutputStreamWriter target,
      OutputStream stream) {
    Tracker streamTracker = getOutputStreamTracker(stream);
    CharSinkTracker tracker = new CharSinkTracker();
    tracker.addTo(TrackerTree.nodeOrUnknown(streamTracker).node("Writer"));
    TrackerRepository.setTracker(context, target, tracker);
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(int)",
      condition = enabledCondition)
  public static void afterWrite1(@Arg("THIS") OutputStreamWriter target, @Arg("ARG0") int c,
    @Arg("INVOCATION") Invocation invocation) {
    Tracker tracker = TrackerRepository.getTracker(context(), target);
    if (tracker != null) {
      TrackerPoint sourcePoint = Invocation.getArgPoint(invocation, 0);
      if (sourcePoint != null) {
        tracker.setSource(tracker.getLength(), 1, sourcePoint);
      }
      ((CharSinkTracker) tracker).append((char) c);
    }
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(char[],int,int)",
      condition = enabledCondition)
  public static void afterWriteCharArrayOffset(@Arg("THIS") OutputStreamWriter target,
      @Arg("ARG0") char[] cbuf, @Arg("ARG1") int off, @Arg("ARG2") int len) {
    Context context = context();
    Tracker tracker = TrackerRepository.getTracker(context, target);
    if (tracker != null) {
      Tracker sourceTracker = TrackerRepository.getTracker(context, cbuf);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((CharSinkTracker) tracker).append(cbuf, off, len);
    }
  }

  @Hook(target = "java.io.OutputStreamWriter",
      method = "void write(java.lang.String,int,int)",
      condition = enabledCondition)
  public static void afterWriteStringOffset(@Arg("THIS") OutputStreamWriter target,
      @Arg("ARG0") String str, @Arg("ARG1") int off, @Arg("ARG2") int len) {
    Context context = context();
    Tracker tracker = TrackerRepository.getTracker(context, target);
    if (tracker != null) {
      Tracker sourceTracker = StringHook.getStringTracker(context, str);
      if (sourceTracker != null) {
        tracker.setSource(tracker.getLength(), len, sourceTracker, off);
      }
      ((CharSinkTracker) tracker).append(str, off, len);
    }
  }

  private static Tracker getOutputStreamTracker(OutputStream os) {
    if (os instanceof FileOutputStream) {
      try {
        return FileDescriptorTrackerRepository.forceGetWriteTracker(((FileOutputStream) os).getFD());
      } catch (IOException e) {
        return null;
      }
    } else if (os instanceof FilterOutputStream) {
      return getOutputStreamTracker(filterOutputStream_out((FilterOutputStream) os));
    }
    return null;
  }

  private static OutputStream filterOutputStream_out(FilterOutputStream o) {
    return (OutputStream) filterOutputStream_out.get(o);
  }
}
