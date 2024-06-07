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

import static com.coekie.flowtracker.web.SourceResource.getAsStream;
import static com.coekie.flowtracker.web.SourceResource.lineToPartMapping;
import static java.util.stream.Collectors.toList;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.web.SourceResource.Line;
import com.coekie.flowtracker.web.SourceResource.SourceResponse;
import com.coekie.flowtracker.web.TrackerResource.TrackerPartResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IContextSource.IOutputSink;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

/** Use Vineflower to decompile bytecode, with mapping of source code line numbers */
public class VineflowerSourceGenerator {

  /** Use ASM to dump the bytecode */
  static Map<Long, SourceResponse> getSource(List<ClassOriginTracker> trackers) {
    if (trackers.isEmpty()) {
      return Map.of();
    }
    ClassLoader loader = trackers.get(0).loader;

    Map<String, Object> properties = new HashMap<>();
    properties.put(IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1");
    properties.put(IFernflowerPreferences.DUMP_CODE_LINES, "1");
    properties.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");

    Fernflower engine = new Fernflower(null, properties, new FernflowerToFTLogger());
    OutputSink sink = new OutputSink();
    engine.addLibrary(new LibraryContextSource(loader));
    engine.addSource(new SourceContextSource(loader, trackers, sink));
    engine.decompileContext();

    Map<Long, SourceResponse> result = new HashMap<>();
    for (ClassOriginTracker tracker : trackers) {
      Map<Integer, List<TrackerPartResponse>> lineToPartMapping = lineToPartMapping(tracker);

      String output = sink.output.get(tracker.className);
      if (output != null) {
        List<Line> lines = output.lines()
            .map((String line) -> toLine(line, lineToPartMapping))
            .collect(toList());

        result.put(tracker.getTrackerId(), new SourceResponse(lines));
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
    if (!comment.chars().allMatch(c -> c >= '0' && c <= '9')) {
      return new Line(null, line + '\n', List.of());
    }
    int lineNumber = Integer.parseInt(comment);
    return new Line(lineNumber, line.substring(0, index) + '\n',
        lineToPartMapping.getOrDefault(lineNumber, List.of()));
  }

  /** Sends Fernflower logs to flowtracker's Logger */
  static class FernflowerToFTLogger extends IFernflowerLogger {
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
  static class SourceContextSource implements IContextSource {
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
  static class LibraryContextSource implements IContextSource {
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

  static class OutputSink implements IOutputSink {
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
}
