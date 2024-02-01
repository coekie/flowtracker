package be.coekaerts.wouter.flowtracker.test;

import java.nio.ByteBuffer;

public class HeapByteBufferTest extends AbstractByteBufferTest {

  @Override ByteBuffer allocate(int capacity) {
    return ByteBuffer.allocate(capacity);
  }

  @Override ByteBuffer allocateTracked(int capacity) {
    return createTrackedHeapBuffer(capacity);
  }

  @Override
  int sliceOffset() {
    return 0;
  }
}
