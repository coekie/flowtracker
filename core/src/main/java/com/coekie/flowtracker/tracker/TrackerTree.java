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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tree of interesting trackers, for the UI */
public class TrackerTree {
  public static final Node ROOT = new Node(null, "<root>", false);
  public static final Node FILES = node("Files");
  public static final Node CLASS = node("Class");

  public static Node node(String name) {
    return ROOT.node(name);
  }

  /** Root node for tracking operations on files */
  public static Node fileNode(String name) {
    try {
      return FILES.pathNode(new File(name).getCanonicalPath());
    } catch (IOException e) {
      return FILES.node("<invalid>").node(name);
    }
  }

  private static boolean isPathSeparator(char c) {
    return c == '/' || c == '\\';
  }

  public static class Node {
    public final Node parent;
    public final String name;

    /**
     * An optional node is hidden if it doesn't have siblings; only shown if necessary for
     * disambiguation
     */
    public final boolean optional;

    private final Map<String, Node> children = new HashMap<>();
    private final List<Tracker> trackers = new ArrayList<>();

    Node(Node parent, String name, boolean optional) {
      this.parent = parent;
      this.name = name;
      this.optional = optional;
    }

    /** Get or create a new child node */
    public synchronized Node node(String name) {
      return children.computeIfAbsent(name, this::createChild);
    }

    /** Get or create a chain of child nodes, for the given path */
    public synchronized Node pathNode(String path) {
      Node node = this;
      int start = 0;
      int length = path.length();

      while (start < length && isPathSeparator(path.charAt(start))) {
        start++;
      }
      int end = start;

      while (end < length) {
        while (end < length && !isPathSeparator(path.charAt(end))) {
          end++;
        }
        node = node.optionalNode(path.substring(start, end));
        while (end < length && isPathSeparator(path.charAt(end))) {
          end++;
        }
        start = end;
      }

      return node;
    }

    /** Get or create a new {@link #optional} child node */
    public synchronized Node optionalNode(String name) {
      return children.computeIfAbsent(name, this::createOptionalChild);
    }

    private Node createChild(String name) {
      return new Node(this, name, false);
    }

    private Node createOptionalChild(String name) {
      return new Node(this, name, true);
    }

    /** Should only be used by {@link Tracker#addTo(Node)} */
    synchronized void internalAddTracker(Tracker tracker) {
      trackers.add(tracker);
    }

    public synchronized List<Node> children() {
      return new ArrayList<>(children.values());
    }

    public synchronized List<Tracker> trackers() {
      return new ArrayList<>(trackers);
    }

    public List<String> path() {
      Node node = this;
      List<String> result = new ArrayList<>();
      while (node.parent != null) {
        result.add(node.name);
        node = node.parent;
      }
      Collections.reverse(result);
      return result;
    }
  }
}
