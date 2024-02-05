package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.nio.ByteBuffer;
import org.junit.Test;

public abstract class AbstractByteBufferTest {
  @Test
  public void testPutOne() {
    ByteBuffer bb = allocate(10);
    bb.position(5);
    FlowTester ft = new FlowTester();
    bb.put(ft.createSourceByte((byte)'x'));

    snapshotBuilder().gap(5 + sliceOffset()).part(ft.theSource(), ft.theSourceIndex(), 1)
        .assertEquals(getTracker(bb));
  }

  @Test
  public void testPutArray() {
    byte[] array = TrackTestHelper.trackedByteArray("abc");

    ByteBuffer bb = allocate(10);
    bb.position(5);
    bb.put(array, 1, 2);

    snapshotBuilder().gap(5 + sliceOffset()).track(array, 1, 2)
        .assertEquals(getTracker(bb));
  }

  @Test
  public void testPutHeapBuffer() {
    ByteBuffer src = createTrackedHeapBuffer(3);

    ByteBuffer bb = allocate(10);
    bb.position(5);
    bb.put(src);

    snapshotBuilder().gap(5 + sliceOffset()).part(getTracker(src), 0, 3)
        .assertEquals(getTracker(bb));
  }

  // overwrite part of a tracker buffer with a direct ByteBuffer which we don't track yet.
  // we have separate tests for a small and large direct ByteBuffer, because DirectByteBuffer.get
  // makes a distinction between buffers smaller or larger than Bits.JNI_COPY_TO_ARRAY_THRESHOLD.
  @Test
  public void testPutDirectBufferGapSmall() {
    ByteBuffer src = ByteBuffer.allocateDirect(3);

    ByteBuffer bb = allocate(10);

    byte[] array = TrackTestHelper.trackedByteArray("abcdef");
    bb.put(array);

    bb.position(1);
    bb.put(src);

    snapshotBuilder().gap(sliceOffset()).track(array, 0, 1).gap(3).track(array, 4, 2)
        .assertEquals(getTracker(bb));
  }

  @Test
  public void testPutDirectBufferGapLarge() {
    ByteBuffer src = ByteBuffer.allocateDirect(20);

    ByteBuffer bb = allocate(30);

    byte[] array = TrackTestHelper.trackedByteArray("abcdefghijklmnopqrstuvwxyz");
    bb.put(array);

    bb.position(1);
    bb.put(src);

    snapshotBuilder().gap(sliceOffset()).track(array, 0, 1).gap(20).track(array, 21, 5)
        .assertEquals(getTracker(bb));
  }

  @Test
  public void testGetArray() {
    ByteBuffer bb = allocateTracked(4);
    byte[] array = new byte[10];

    bb.position(1);
    bb.get(array, 5, 3);

    snapshotBuilder().gap(5).part(getTracker(bb), 1 + sliceOffset(), 3)
        .assertTrackerOf(array);
  }

  @Test
  public void testGetOne() {
    ByteBuffer bb = allocateTracked(4);
    bb.position(2);
    TrackerPoint point = FlowTester.getByteSourcePoint(bb.get());
    assertSame(getTracker(bb), point.tracker);
    assertEquals(2 + sliceOffset(), point.index);
  }

  @Test
  public void testCompact() {
    ByteBuffer bb = allocate(10);
    byte[] array = TrackTestHelper.trackedByteArray("abcde");
    bb.put(array);
    bb.position(2);
    bb.compact();

    snapshotBuilder().gap(sliceOffset()).track(array, 2, 3)
        .assertEquals(getTracker(bb));
  }

  /**
   * Create a heap (non-direct) ByteBuffer tracked by a {@link FixedOriginTracker}.
   */
  ByteBuffer createTrackedHeapBuffer(int capacity) {
    return ByteBuffer.wrap(TrackTestHelper.trackedByteArray("x".repeat(capacity)));
  }

  Tracker getTracker(ByteBuffer bb) {
    assertFalse(bb.isDirect()); // direct ByteBuffers not supported here yet
    return TrackerRepository.getTracker(bb.array());
  }

  /** Create a ByteBuffer with given capacity, with no tracker yet */
  abstract ByteBuffer allocate(int capacity);

  /** Create a ByteBuffer with given capacity, for which the content is already tracked */
  abstract ByteBuffer allocateTracked(int capacity);

  /** The offset for ByteBuffers returned by {@link #allocate(int)} and friends */
  abstract int sliceOffset();
}