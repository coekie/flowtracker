package be.coekaerts.wouter.flowtracker.tracker;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * A dynamically growing byte array
 */
public class ByteSequence extends ByteArrayOutputStream {
  public ByteBuffer getByteContent() {
    return ByteBuffer.wrap(buf, 0, size());
  }
}
