package be.coekaerts.wouter.flowtracker.tracker;

import java.nio.channels.Channel;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class ChannelTrackerRepository {
  // like TrackerRepository, ideally these would be concurrent weak identity hash maps?
  private static final Map<Channel, TrackerPair> map =
      Collections.synchronizedMap(new IdentityHashMap<Channel, TrackerPair>());

  public static Tracker getReadTracker(Channel channel) {
    if (!Trackers.isActive()) return null;
    TrackerPair pair = map.get(channel);
    return pair == null ? null : pair.readTracker;
  }

  public static Tracker getWriteTracker(Channel channel) {
    if (!Trackers.isActive()) return null;
    TrackerPair pair = map.get(channel);
    return pair == null ? null : pair.writeTracker;
  }

  public static void createTracker(Channel channel, String descriptor, boolean read,
      boolean write) {
    ByteOriginTracker readTracker;
    if (read) {
      readTracker = new ByteOriginTracker();
      readTracker.initDescriptor("Read channel for " + descriptor, null);
      InterestRepository.interestTrackerCreated(readTracker);
    } else {
      readTracker = null;
    }

    ByteSinkTracker writeTracker;
    if (write) {
      writeTracker = new ByteSinkTracker();
      writeTracker.initDescriptor("Write channel for " + descriptor, null);
      InterestRepository.interestTrackerCreated(writeTracker);
    } else {
      writeTracker = null;
    }

    TrackerPair pair = new TrackerPair(readTracker, writeTracker);
    map.put(channel, pair);
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
