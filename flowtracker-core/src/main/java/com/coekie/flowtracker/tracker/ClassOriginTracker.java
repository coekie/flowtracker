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
  private static final List<ClassOriginTracker> trackers = new ArrayList<>();

  /** Cache for finding ClassOriginTracker from a java.lang.Class */
  private static final ClassValue<ClassOriginTracker> byClassCache = new ClassValue<>() {
    @Override
    protected ClassOriginTracker computeValue(Class<?> type) {
      return getUncached(type);
    }
  };

  public final int classId;
  private final StringBuilder content = new StringBuilder();

  private ClassOriginTracker(String className) {
    this.classId = trackers.size();
    content.append("class ").append(className.replace('/', '.')).append('\n');
  }

  public static synchronized ClassOriginTracker registerClass(String className) {
    ClassOriginTracker tracker = new ClassOriginTracker(className);
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
    if (trackers.isEmpty()) {
      return registerClass(internalName);
    } else {
      // TODO if there are multiple classes with the same name, from different ClassLoaders, then
      //   we should pick the right one. To do that we'd have to pass along the ClassLoader in
      //   transformers, ConstantsTransformation and friends. For now we just don't care.
      return (ClassOriginTracker) trackers.get(0);
    }
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

  public synchronized ClassConstant registerConstant(int value) {
    content.append("  ");
    int offset = content.length();
    if (value >= 32 && value < 127) { // printable ascii characters
      content.append((char) value);
    } else {
      content.append("0x").append(Integer.toHexString(value))
          .append(" (").append(value).append(')');
    }
    ClassConstant result = new ClassConstant(classId, offset, content.length() - offset);
    content.append('\n');
    return result;
  }

  public synchronized int registerConstantString(String value) {
    content.append("  ");
    int offset = content.length();
    content.append(value).append('\n');
    return offset;
  }

  public static class ClassConstant {
    public final int classId;
    public final int offset;
    public final int length;

    public ClassConstant(int classId, int offset, int length) {
      this.classId = classId;
      this.offset = offset;
      this.length = length;
    }

    @Override
    public String toString() {
      return "ClassConstant{" +
          "classId=" + classId +
          ", offset=" + offset +
          ", length=" + length +
          '}';
    }
  }
}
