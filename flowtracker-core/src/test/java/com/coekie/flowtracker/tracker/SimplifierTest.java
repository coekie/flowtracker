package com.coekie.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth;
import org.junit.Test;

public class SimplifierTest {
  private final Tracker source = new FixedOriginTracker(1000);
  private final Tracker source2 = new FixedOriginTracker(1000);

  @Test
  public void testSimpleMerge() {
    Truth.assertThat(TrackerSnapshot.snapshot().part(2, source, 1).part(2, source, 3).build().simplify())
        .isEqualTo(TrackerSnapshot.snapshot().part(4, source, 1).build());
  }

  @Test
  public void testSimpleMergeGrowth() {
    Truth.assertThat(TrackerSnapshot.snapshot()
        .part(10, source, 1, Growth.of(10, 1))
        .part(10, source, 2, Growth.of(10, 1)).build().simplify())
        .isEqualTo(TrackerSnapshot.snapshot().part(20, source, 1, Growth.of(10, 1)).build());
  }

  @Test
  public void testExpandWithGrowth() {
    Truth.assertThat(TrackerSnapshot.snapshot()
        .part(2, source, 1, Growth.of(2, 1))
        .part(3, source, 2, Growth.of(3, 1)).build().simplify())
        .isEqualTo(TrackerSnapshot.snapshot().part(5, source, 1, Growth.of(5, 2)).build());
  }

  @Test
  public void testRepeated() {
    Truth.assertThat(TrackerSnapshot.snapshot()
        .part(1, source, 1)
        .part(1, source, 1)
        .part(1, source, 1)
        .build().simplify())
        .isEqualTo(TrackerSnapshot.snapshot().part(3, source, 1, Growth.of(3, 1)).build());
  }

  @Test
  public void testDoNotMergeWrongSourceIndex() {
    assertNoChange(TrackerSnapshot.snapshot().part(2, source, 1).part(2, source, 5));
  }

  @Test
  public void testDoNotMergeDifferentTracker() {
    assertNoChange(TrackerSnapshot.snapshot().part(2, source, 1).part(2, source2, 3));
  }

  private void assertNoChange(TrackerSnapshot.Builder snapshotBuilder) {
    TrackerSnapshot snapshot = snapshotBuilder.build();
    assertThat(snapshot.simplify()).isEqualTo(snapshot);
  }
}