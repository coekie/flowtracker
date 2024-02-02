package be.coekaerts.wouter.flowtracker.tracker;

import java.io.FileDescriptor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class FileDescriptorTrackerRepository {
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
    TrackerPair pair = map.get(fd);
    return pair == null ? null : pair.writeTracker;
  }

  public static boolean hasTracker(FileDescriptor fd) {
    return map.containsKey(fd);
  }

  public static void createTracker(FileDescriptor fd, String descriptor, boolean read,
      boolean write, TrackerTree.Node node) {
    ByteOriginTracker readTracker;
    if (read) {
      readTracker = new ByteOriginTracker();
      readTracker.initDescriptor((write ? "Read " : "") + descriptor);
      InterestRepository.interestTrackerCreated(readTracker);
      if (node != null) {
        readTracker.addTo(node);
      }
    } else {
      readTracker = null;
    }

    ByteSinkTracker writeTracker;
    if (write) {
      writeTracker = new ByteSinkTracker();
      writeTracker.initDescriptor((read ? "Write " : "") + descriptor);
      InterestRepository.interestTrackerCreated(writeTracker);
      if (node != null) {
        writeTracker.addTo(node);
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
