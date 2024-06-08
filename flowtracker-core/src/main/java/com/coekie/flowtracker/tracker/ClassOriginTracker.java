package com.coekie.flowtracker.tracker;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracker for constants (literals) defined in a class.
 * <p>
 * Ideally we'd have a proper model of class members. We could then render that, when requested.
 * Then members could be added only if they've actually being used (tracked). We could even render
 * it in a nicer way in the UI.
 * But for now we represent the whole class as a long String, built at instrumentation time, that
 * contains everything that _could_ be tracked. That's simpler to implement.
 */
public class ClassOriginTracker extends OriginTracker implements CharContentTracker {
  /**
   * Content in the tracker for untracked values; that is values where we didn't track where they
   * originally came from, but we point to where in the code we started tracking them.
   */
  public static final String FALLBACK = "<?>";

  private static final List<ClassOriginTracker> trackers = new ArrayList<>();

  /** Cache for finding ClassOriginTracker from a java.lang.Class */
  private static final ClassValue<ClassOriginTracker> byClassCache = new ClassValue<>() {
    @Override
    protected ClassOriginTracker computeValue(Class<?> type) {
      return getUncached(type);
    }
  };

  public final int classId;
  public final ClassLoader loader;
  public final String className;
  public final String sourceFile;
  private final StringBuilder content = new StringBuilder();
  private final List<ClassEntry> entries = new ArrayList<>();

  /** Map field name to offset in content */
  private final ConcurrentMap<String, Integer> fields = new ConcurrentHashMap<>();
  /** Map method name to offset in content */
  private final ConcurrentMap<String, Integer> methods = new ConcurrentHashMap<>();

  private ClassOriginTracker(ClassLoader loader, String className, String sourceFile) {
    this.loader = loader;
    this.className = className;
    this.sourceFile = sourceFile;
    this.classId = trackers.size();
    content.append("class ").append(className.replace('/', '.')).append('\n');
  }

  public static synchronized ClassOriginTracker registerClass(
      ClassLoader loader, String className, String sourceFile) {
    ClassOriginTracker tracker = new ClassOriginTracker(loader, className, sourceFile);
    trackers.add(tracker);
    tracker.addTo(TrackerTree.CLASS.pathNode(className));
    return tracker;
  }

  public static ClassOriginTracker get(int classId) {
    return trackers.get(classId);
  }

  public static ClassOriginTracker get(Class<?> clazz) {
    return byClassCache.get(clazz);
  }

  private static ClassOriginTracker getUncached(Class<?> clazz) {
    String internalName = clazz.getName().replace('.', '/');
    List<Tracker> trackers = TrackerTree.CLASS.pathNode(internalName).trackers();
    for (Tracker tracker : trackers) {
      if (tracker instanceof ClassOriginTracker) {
        ClassOriginTracker coTracker = (ClassOriginTracker) tracker;
        if (coTracker.loader == clazz.getClassLoader()) {
          return coTracker;
        }
      }
    }
    return registerClass(clazz.getClassLoader(), internalName, null);
  }

  @Override
  public CharSequence getContent() {
    return content;
  }

  @Override
  public int getLength() {
    return 0;
  }

  public synchronized void startMethod(String method) {
    content.append(method).append(":\n");
  }

  public synchronized ClassEntry registerConstant(int value, int line) {
    appendConstantPrefix(line);
    int offset = content.length();
    if (value >= 32 && value < 127) { // printable ascii characters
      content.append((char) value);
    } else {
      content.append("0x").append(Integer.toHexString(value))
          .append(" (").append(value).append(')');
    }
    ClassEntry result = registerEntry(offset, content.length() - offset, line);
    content.append('\n');
    return result;
  }

  public synchronized ClassEntry registerFallback(int line) {
    appendConstantPrefix(line);
    int offset = content.length();
    content.append(FALLBACK);
    ClassEntry result = registerEntry(offset, content.length() - offset, line);
    content.append('\n');
    return result;
  }

  public synchronized int registerConstantString(String value, int line) {
    appendConstantPrefix(line);
    int offset = content.length();
    content.append(value);
    registerEntry(offset, content.length() - offset, line);
    content.append('\n');
    return offset;
  }

  private void appendConstantPrefix(int line) {
    content.append("  ");
    if (line != -1) {
      content.append("(line ").append(line).append(") ");
    }
  }

  private ClassEntry registerEntry(int offset, int length, int line) {
    ClassEntry result = new ClassEntry(classId, offset, length, line);
    entries.add(result);
    return result;
  }

  public synchronized int getFieldOffset(String name) {
    return fields.computeIfAbsent(name, n -> {
      content.append("field: ");
      int offset = content.length();
      content.append(name);
      content.append('\n');
      return offset;
    });
  }

  public synchronized int getMethodOffset(String name) {
    return methods.computeIfAbsent(name, n -> {
      content.append("method: ");
      int offset = content.length();
      content.append(name);
      content.append('\n');
      return offset;
    });
  }

  /** Pushes line number information to `consumer` */
  public synchronized void pushLineNumbers(LineNumberConsumer consumer) {
    for (ClassEntry entry : entries) {
      consumer.line(entry.offset, entry.offset + entry.length, entry.line);
    }
  }

  /**
   * An entry in the class that trackers can refer to, e.g. a constant, or pointing to a piece of
   * code (fallback).
   */
  public static class ClassEntry {
    public final int classId;
    public final int offset;
    public final int length;
    public final int line;

    private ClassEntry(int classId, int offset, int length, int line) {
      this.classId = classId;
      this.offset = offset;
      this.length = length;
      this.line = line;
    }

    @Override
    public String toString() {
      return "ClassEntry{" +
          "classId=" + classId +
          ", offset=" + offset +
          ", length=" + length +
          '}';
    }
  }

  /** Consumer for line number information for {@link #pushLineNumbers(LineNumberConsumer)}. */
  public interface LineNumberConsumer {
    /** Indicates that the content from index `start` to `end` maps to `line` in the source code */
    void line(int start, int end, int line);
  }
}
