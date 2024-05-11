package com.coekie.flowtracker.test;

import java.nio.ByteBuffer;

/**
 * Test handling of heap ByteBuffers that have an offset; that came from {@link ByteBuffer#slice()}
 */
public class HeapByteBufferSliceTest extends AbstractByteBufferTest {

  @Override ByteBuffer allocate(int capacity) {
    ByteBuffer bb = ByteBuffer.allocate(capacity + sliceOffset());
    bb.position(sliceOffset());
    return bb.slice();
  }

  @Override ByteBuffer allocateTracked(int capacity) {
    ByteBuffer bb = createTrackedHeapBuffer(capacity + sliceOffset());
    bb.position(sliceOffset());
    return bb.slice();
  }

  @Override int sliceOffset() {
    return 9;
  }
}
