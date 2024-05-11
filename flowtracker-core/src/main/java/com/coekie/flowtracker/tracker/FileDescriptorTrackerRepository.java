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

import java.io.FileDescriptor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class FileDescriptorTrackerRepository {
  public static final String READ = "Read";
  public static final String WRITE = "Write";

  // like TrackerRepository, ideally these would be concurrent weak identity hash maps?
  private static final Map<FileDescriptor, TrackerPair> map =
      Collections.synchronizedMap(new IdentityHashMap<>());

  public static ByteOriginTracker getReadTracker(FileDescriptor fd) {
    if (!Trackers.isActive()) return null;
    TrackerPair pair = map.get(fd);
    return pair == null ? null : pair.readTracker;
  }

  public static ByteSinkTracker getWriteTracker(FileDescriptor fd) {
    if (!Trackers.isActive()) return null;
    return forceGetWriteTracker(fd);
  }

  /** Get tracker for writes, even when not {@link Trackers#isActive()} */
  public static ByteSinkTracker forceGetWriteTracker(FileDescriptor fd) {
    TrackerPair pair = map.get(fd);
    return pair == null ? null : pair.writeTracker;
  }

  public static boolean hasTracker(FileDescriptor fd) {
    return map.containsKey(fd);
  }

  public static void createTracker(FileDescriptor fd, boolean read, boolean write,
      TrackerTree.Node node) {
    // if we're reading *and* writing, then create separate nodes to distinguish the two.
    boolean twoNodes = read && write;
    ByteOriginTracker readTracker;
    if (read) {
      readTracker = new ByteOriginTracker();
      if (node != null) {
        readTracker.addTo(twoNodes ? node.optionalNode(READ) : node);
      }
    } else {
      readTracker = null;
    }

    ByteSinkTracker writeTracker;
    if (write) {
      writeTracker = new ByteSinkTracker();
      if (node != null) {
        writeTracker.addTo(twoNodes ? node.optionalNode(WRITE) : node);
      }
    } else {
      writeTracker = null;
    }

    TrackerPair pair = new TrackerPair(readTracker, writeTracker);
    map.put(fd, pair);
  }

  private static class TrackerPair {
    private final ByteOriginTracker readTracker;
    private final ByteSinkTracker writeTracker;

    private TrackerPair(ByteOriginTracker readTracker, ByteSinkTracker writeTracker) {
      this.readTracker = readTracker;
      this.writeTracker = writeTracker;
    }
  }
}
