package com.coekie.flowtracker.web;

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

import static com.coekie.flowtracker.web.CodeResource.getAsStream;
import static com.coekie.flowtracker.web.CodeResource.lineToPartMapping;
import static java.util.stream.Collectors.toList;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.Trackers;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.web.CodeResource.CodeResponse;
import com.coekie.flowtracker.web.CodeResource.Line;
import com.coekie.flowtracker.web.TrackerResource.TrackerPartResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IContextSource.IOutputSink;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.struct.StructClass;

/** Use Vineflower to decompile bytecode, with mapping of source code line numbers */
public class VineflowerCodeGenerator {

  /** Use ASM to dump the bytecode */
  static Map<Long, CodeResponse> getCode(List<ClassOriginTracker> trackers) {
    if (trackers.isEmpty()) {
      return Map.of();
    }
    ClassLoader loader = trackers.get(0).loader;

    Map<String, Object> properties = new HashMap<>();
    properties.put(IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1");
    properties.put(IFernflowerPreferences.DUMP_CODE_LINES, "1");
    properties.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");

    Fernflower engine = new MyFernflower(null, properties, new FernflowerToFTLogger());
    OutputSink sink = new OutputSink();
    engine.addLibrary(new LibraryContextSource(loader));
    engine.addSource(new SourceContextSource(loader, trackers, sink));
    engine.decompileContext();

    Map<Long, CodeResponse> result = new HashMap<>();
    for (ClassOriginTracker tracker : trackers) {
      Map<Integer, List<TrackerPartResponse>> lineToPartMapping = lineToPartMapping(tracker);

      String output = sink.output.get(tracker.className);
      if (output != null) {
        List<Line> lines = Stream.concat(
                Stream.of(new Line(null, "// Decompiled by Vineflower\n\n", List.of())),
                output.lines()
                    .map((String line) -> toLine(line, lineToPartMapping))
            )
            .collect(toList());

        result.put(tracker.getTrackerId(), new CodeResponse(lines));
      }
    }
    return result;
  }

  private static Line toLine(String line, Map<Integer,
      List<TrackerPartResponse>> lineToPartMapping) {
    int index = line.indexOf("// ");
    if (index == -1) {
      return new Line(null, line + '\n', List.of());
    }
    String comment = line.substring(index + 3);
    if (!comment.chars().allMatch(c -> (c >= '0' && c <= '9') || c == ' ')) {
      return new Line(null, line + '\n', List.of());
    }
    List<Integer> lineNumbers = Stream.of(comment.split(" "))
        .map(Integer::parseInt)
        .collect(toList());
    // response format currently only allows one line number
    Integer lineNumber = lineNumbers.isEmpty() ? null : lineNumbers.get(0);

    List<TrackerPartResponse> parts = Stream.of(comment.split(" "))
        .map(Integer::parseInt)
        .flatMap(n -> lineToPartMapping.getOrDefault(n, List.of()).stream())
        .collect(toList());
    return new Line(lineNumber, line.substring(0, index) + '\n', parts);
  }

  /** Sends Fernflower logs to flowtracker's Logger */
  private static class FernflowerToFTLogger extends IFernflowerLogger {
    private static final Logger logger = new Logger("Vineflower");

    @Override
    public void writeMessage(String message, Severity severity) {
      logger.info(severity.prefix + message);
    }

    @Override
    public void writeMessage(String message, Severity severity, Throwable t) {
      if (severity == Severity.ERROR) {
        logger.error(t, message);
      } else {
        logger.info(severity.prefix + message);
      }
    }
  }

  /** Provider Fernflower with the class that needs to be decompiled */
  private static class SourceContextSource implements IContextSource {
    private final ClassLoader classLoader;
    private final List<ClassOriginTracker> trackers;
    private final OutputSink sink;

    SourceContextSource(ClassLoader classLoader, List<ClassOriginTracker> trackers,
        OutputSink sink) {
      this.classLoader = classLoader;
      this.trackers = trackers;
      this.sink = sink;
    }

    @Override
    public String getName() {
      return SourceContextSource.class.getSimpleName();
    }

    @Override
    public Entries getEntries() {
      // TODO do we also need to include nested classes here?
      //   "WARN:  Nested class [...] missing!"
      return new Entries(
          trackers.stream()
              .map(tracker -> Entry.atBase(tracker.className))
              .collect(toList()),
          List.of(), List.of());
    }

    @Override
    public InputStream getInputStream(String resource) {
      return getAsStream(classLoader, resource);
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
      return sink;
    }
  }

  /** Lets Fernflower load other classes, that are used by decompiled classes */
  private static class LibraryContextSource implements IContextSource {
    private final ClassLoader classLoader;

    LibraryContextSource(ClassLoader classLoader) {
      this.classLoader = classLoader;
    }

    @Override
    public String getName() {
      return LibraryContextSource.class.getSimpleName();
    }

    @Override
    public Entries getEntries() {
      return Entries.EMPTY; // ok because isLazy()
    }

    @Override
    public boolean isLazy() {
      return true;
    }

    @Override
    public InputStream getInputStream(String resource) {
      return getAsStream(classLoader, resource);
    }
  }

  private static class OutputSink implements IOutputSink {
    private final Map<String, String> output = new HashMap<>();

    @Override
    public void begin() {
    }

    @Override
    public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
      output.put(qualifiedName, content);
    }

    @Override
    public void acceptDirectory(String directory) {
    }

    @Override
    public void acceptOther(String path) {
    }

    @Override
    public void close() {
    }
  }

  private static class MyFernflower extends Fernflower {
    MyFernflower(IResultSaver saver, Map<String, Object> customProperties,
        IFernflowerLogger logger) {
      super(saver, customProperties, logger);
    }

    @Override
    public String getClassContent(StructClass cl) {
      // disable tracking, for performance. Note that Vineflower runs this in parallel in multiple
      // threads, so suspending it just in the thread that started it (e.g. in getSource) wouldn't
      // do it.
      Trackers.suspendOnCurrentThread();
      try {
        return super.getClassContent(cl);
      } finally {
        Trackers.unsuspendOnCurrentThread();
      }
    }

    @Override
    public void processClass(StructClass cl) throws IOException {
      Trackers.suspendOnCurrentThread();
      try {
        super.processClass(cl);
      } finally {
        Trackers.unsuspendOnCurrentThread();
      }
    }
  }
}
