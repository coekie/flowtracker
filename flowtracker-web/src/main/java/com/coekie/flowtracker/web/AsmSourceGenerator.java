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

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.SourceResource.Line;
import com.coekie.flowtracker.web.SourceResource.SourceResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

/** Use ASM to dump bytecode to text form, with mapping of source code line numbers */
public class AsmSourceGenerator {

  /** Use ASM to dump the bytecode */
  static SourceResponse getSource(ClassOriginTracker tracker) throws IOException {
    try (InputStream is = getAsStream(tracker.loader, tracker.className + ".class")) {
      if (is == null) {
        return null;
      }

      LineTextifier textifier = new LineTextifier();
      TraceClassVisitor traceClassVisitor =
          new TraceClassVisitor(null, textifier, null);
      ClassReader reader = new ClassReader(is);
      reader.accept(traceClassVisitor, ClassReader.SKIP_FRAMES);

      return new SourceResponse(new LineListBuilder().append(textifier.text).flush().lines);
    }
  }

  /** {@link Textifier} that tracks line numbers by adding {@link LineMarker}s */
  private static class LineTextifier extends Textifier {
    /** Index in {@link #text} of the previous label */
    private int prevLabelIndex = -2;
    /** Active line number */
    private Integer prevLine = null;

    private LineTextifier() {
      super(Opcodes.ASM9);
    }

    @Override
    public void visitLabel(Label label) {
      prevLabelIndex = text.size();
      super.visitLabel(label);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
      setLine(line);
      super.visitLineNumber(line, start);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start,
        Label end, int index) {
      setLine(null);
      super.visitLocalVariable(name, descriptor, signature, start, end, index);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      setLine(null);
      super.visitMaxs(maxStack, maxLocals);
    }

    private void setLine(Integer line) {
      if (Objects.equals(prevLine, line)) {
        return;
      }
      prevLine = line;

      // consider the line number to also apply to the label before it
      if (prevLabelIndex == text.size() - 1) {
        // put the label in the next spot
        text.add(text.get(text.size() - 1));
        // and overwrite the previous one with the line marker
        text.set(text.size() - 2, new LineMarker(line));
      } else {
        text.add(new LineMarker(line));
      }
    }

    @Override
    protected Textifier createTextifier() {
      return new LineTextifier();
    }
  }

  /**
   * Convert the {@link Textifier#text} list to {@link Line}s.
   * Plays a similar role as {@link Textifier#print(PrintWriter)}, also recursively flattening
   * the list.
   */
  private static class LineListBuilder {
    private final List<Line> lines = new ArrayList<>();
    private final StringBuilder sb = new StringBuilder();
    private Integer currentLine = null;

    LineListBuilder append(List<?> list) {
      for (Object o : list) {
        if (o instanceof List) {
          append((List<?>) o);
        } else if (o instanceof LineMarker) {
          LineMarker marker = (LineMarker) o;
          if (!Objects.equals(currentLine, marker.line)) {
            flush();
            currentLine = marker.line;
          }
        } else {
          sb.append(o.toString());
        }
      }
      return this;
    }

    LineListBuilder flush() {
      if (sb.length() != 0) {
        lines.add(new Line(currentLine, sb.toString()));
        sb.setLength(0);
      }
      return this;
    }
  }

  /**
   * Marker object that we add into {@link Textifier#text} to mark the start of a new line number.
   */
  private static class LineMarker {
    final Integer line;

    LineMarker(Integer line) {
      this.line = line;
    }

    @Override
    public String toString() {
      return "--- Line " + line + " ---\n";
    }
  }
}
