package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot;
import java.nio.ByteBuffer;
import org.junit.Test;

public abstract class AbstractByteBufferTest {
  @Test
  public void testPutOne() {
    ByteBuffer bb = allocate(10);
    bb.position(5);
    FlowTester ft = new FlowTester();
    bb.put(ft.createSourceByte((byte)'x'));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(5 + sliceOffset()).part(ft.point()));
  }

  @Test
  public void testPutChar() {
    ByteBuffer bb = allocate(10);
    bb.position(5);
    FlowTester ft = new FlowTester();
    bb.putChar(ft.createSourceChar('x'));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(5 + sliceOffset()).part(2, ft.point()));
  }

  @Test
  public void testPutCharPosition() {
    ByteBuffer bb = allocate(10);
    bb.position(5); // irrelevant
    FlowTester ft = new FlowTester();
    bb.putChar(3, ft.createSourceChar('x'));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(3 + sliceOffset()).part(2, ft.point()));
  }

  @Test
  public void testPutShort() {
    ByteBuffer bb = allocate(10);
    bb.position(5);
    FlowTester ft = new FlowTester();
    bb.putShort(ft.createSourceShort((short)'x'));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(5 + sliceOffset()).part(2, ft.point()));
  }

  @Test
  public void testPutShortPosition() {
    ByteBuffer bb = allocate(10);
    bb.position(5); // irrelevant
    FlowTester ft = new FlowTester();
    bb.putShort(3, ft.createSourceShort((short)'x'));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(3 + sliceOffset()).part(2, ft.point()));
  }

  @Test
  public void testPutInt() {
    ByteBuffer bb = allocate(10);
    bb.position(5);
    FlowTester ft = new FlowTester();
    bb.putInt(ft.createSourceInt(123));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(5 + sliceOffset()).part(4, ft.point()));
  }

  @Test
  public void testPutIntPosition() {
    ByteBuffer bb = allocate(10);
    bb.position(5); // irrelevant
    FlowTester ft = new FlowTester();
    bb.putInt(3, ft.createSourceInt(123));

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(3 + sliceOffset()).part(4, ft.point()));
  }

  @Test
  public void testPutArray() {
    byte[] array = TrackTestHelper.trackedByteArray("abc");

    ByteBuffer bb = allocate(10);
    bb.position(5);
    bb.put(array, 1, 2);

    assertThatTracker(bbTracker(bb)).matches(snapshot().gap(5 + sliceOffset()).track(2, array, 1));
  }

  @Test
  public void testPutHeapBuffer() {
    ByteBuffer src = createTrackedHeapBuffer(3);

    ByteBuffer bb = allocate(10);
    bb.position(5);
    bb.put(src);

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(5 + sliceOffset()).part(3, bbTracker(src), 0));
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

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(sliceOffset()).track(1, array, 0).gap(3).track(2, array, 4));
  }

  @Test
  public void testPutDirectBufferGapLarge() {
    ByteBuffer src = ByteBuffer.allocateDirect(20);

    ByteBuffer bb = allocate(30);

    byte[] array = TrackTestHelper.trackedByteArray("abcdefghijklmnopqrstuvwxyz");
    bb.put(array);

    bb.position(1);
    bb.put(src);

    assertThatTracker(bbTracker(bb)).matches(
        snapshot().gap(sliceOffset()).track(1, array, 0).gap(20).track(5, array, 21));
  }

  @Test
  public void testGetArray() {
    ByteBuffer bb = allocateTracked(4);
    byte[] array = new byte[10];

    bb.position(1);
    bb.get(array, 5, 3);

    TrackerSnapshot.assertThatTrackerOf(array).matches(
        snapshot().gap(5).part(3, bbTracker(bb), 1 + sliceOffset()));
  }

  @Test
  public void testGetOne() {
    ByteBuffer bb = allocateTracked(4);
    bb.position(2);
    TrackerPoint point = FlowTester.getByteSourcePoint(bb.get());
    assertThat(point.tracker).isSameInstanceAs(bbTracker(bb));
    assertThat(point.index).isEqualTo(2 + sliceOffset());
  }

  @Test
  public void testCompact() {
    ByteBuffer bb = allocate(10);
    byte[] array = TrackTestHelper.trackedByteArray("abcde");
    bb.put(array);
    bb.position(2);
    bb.compact();

    assertThatTracker(bbTracker(bb)).matches(snapshot().gap(sliceOffset()).track(3, array, 2));
  }

  /**
   * Create a heap (non-direct) ByteBuffer tracked by a {@link FixedOriginTracker}.
   */
  ByteBuffer createTrackedHeapBuffer(int capacity) {
    return ByteBuffer.wrap(TrackTestHelper.trackedByteArray("x".repeat(capacity)));
  }

  Tracker bbTracker(ByteBuffer bb) {
    assertThat(bb.isDirect()).isFalse(); // direct ByteBuffers not supported here yet
    return TrackerRepository.getTracker(bb.array());
  }

  /** Create a ByteBuffer with given capacity, with no tracker yet */
  abstract ByteBuffer allocate(int capacity);

  /** Create a ByteBuffer with given capacity, for which the content is already tracked */
  abstract ByteBuffer allocateTracked(int capacity);

  /** The offset for ByteBuffers returned by {@link #allocate(int)} and friends */
  abstract int sliceOffset();
}
