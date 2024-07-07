package com.coekie.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Test {@link TwinSynchronization} and how sinks and origins use it.
 */
public class TwinSynchronizationTest {
  @Test
  public void testByteOriginAndSink() {
    ByteSinkTracker sink = new ByteSinkTracker();
    ByteOriginTracker origin = new ByteOriginTracker();
    origin.initTwin(sink);

    origin.append((byte) 'o');
    origin.append((byte) 'o');
    sink.append("sss".getBytes(), 0, 3);
    origin.append("ooooo".getBytes(), 0, 5);
    sink.append("ssssssssss".getBytes(), 0, 10);

    assertThat(toString(origin.twinSync())).isEqualTo("@s 0 2, @o 2 3, @s 3 7");
  }

  @Test
  public void testCharOriginAndSink() {
    CharSinkTracker sink = new CharSinkTracker();
    CharOriginTracker origin = new CharOriginTracker();
    origin.initTwin(sink);

    origin.append('o');
    origin.append('o');
    sink.append("sss", 0, 3);
    origin.append("ooooo");
    sink.append("ssssssssss", 0, 10);

    assertThat(toString(origin.twinSync())).isEqualTo("@s 0 2, @o 2 3, @s 3 7");
  }

  private String toString(TwinSynchronization twinSync) {
    return twinSync.markers.stream()
        .map(m -> (m.to instanceof OriginTracker ? "@o " : "@s ") + m.toIndex + " " + m.fromIndex)
        .collect(Collectors.joining(", "));
  }
}