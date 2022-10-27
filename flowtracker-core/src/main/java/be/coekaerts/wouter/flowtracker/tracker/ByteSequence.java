package be.coekaerts.wouter.flowtracker.tracker;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * A dynamically growing byte array
 */
class ByteSequence extends ByteArrayOutputStream {
  ByteBuffer getByteContent() {
    return ByteBuffer.wrap(buf, 0, size());
  }
}
