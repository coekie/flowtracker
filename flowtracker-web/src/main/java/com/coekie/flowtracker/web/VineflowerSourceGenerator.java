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

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.web.SourceResource.Line;
import com.coekie.flowtracker.web.SourceResource.SourceResponse;
import com.coekie.flowtracker.web.TrackerResource.TrackerPartResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IContextSource.IOutputSink;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

/** Use Vineflower to decompile bytecode, with mapping of source code line numbers */
public class VineflowerSourceGenerator {

  /** Use ASM to dump the bytecode */
  static SourceResponse getSource(ClassOriginTracker tracker) throws IOException {
    try (InputStream is = getAsStream(tracker.loader, tracker.className + ".class")) {
      if (is == null) {
        return null;
      }

      Map<String, Object> properties = new HashMap<>();
      properties.put(IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1");
      properties.put(IFernflowerPreferences.DUMP_CODE_LINES, "1");
      properties.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");

      Fernflower engine = new Fernflower(null, properties, new FernflowerToFTLogger());
      OutputSink sink = new OutputSink();
      engine.addLibrary(new LibraryContextSource(tracker.loader));
      engine.addSource(new SourceContextSource(tracker.loader, tracker.className, sink));
      engine.decompileContext();

      Map<Integer, List<TrackerPartResponse>> lineToPartMapping = lineToPartMapping(tracker);

      List<Line> lines = sink.output.get(tracker.className).lines()
          .map((String line) -> toLine(line, lineToPartMapping))
          .collect(Collectors.toList());

      return new SourceResponse(lines);
    }
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
      if (severity == Severity.ERROR) {
        logger.error(message);
      } else {
        logger.info(severity.prefix + message);
      }
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
    private final String className;
    private final OutputSink sink;

    SourceContextSource(ClassLoader classLoader, String className, OutputSink sink) {
      this.classLoader = classLoader;
      this.className = className;
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
      return new Entries(List.of(Entry.atBase(className)), List.of(), List.of());
    }

    @Override
    public InputStream getInputStream(String resource) {
      System.out.println("MySourceContextSource.getInputStream " + resource);
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
//      System.out.println("MyOutputSink.acceptClass " + qualifiedName + " " + fileName + "\n"
//          + content);
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
