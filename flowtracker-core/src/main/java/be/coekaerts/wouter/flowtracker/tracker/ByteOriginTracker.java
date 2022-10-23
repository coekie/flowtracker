package be.coekaerts.wouter.flowtracker.tracker;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Tracker for reading from a source of bytes.
 *
 * @see ContentTracker
 */
public class ByteOriginTracker extends OriginTracker {
  private final ByteSequence content = new ByteSequence();

  public void append(byte b) {
    content.write(b);
  }

  public void append(byte[] cbuf, int offset, int len) {
    content.write(cbuf, offset, len);
  }

  // TODO show content of ByteOriginTracker in UI
//  @Override public boolean supportsContent() {
//    return true;
//  }
//
//  @Override public CharSequence getContent() {
//    return content;
//  }

  public ByteBuffer getByteContent() {
    return content.getByteContent();
  }

  @Override
  public int getLength() {
    return content.size();
  }

  // using ByteArrayOutputStream to store a dynamically growing byte array
  private static class ByteSequence extends ByteArrayOutputStream {
    byte[] getBuf() {
      return buf;
    }

    ByteBuffer getByteContent() {
      return ByteBuffer.wrap(buf, 0, size());
    }
  }
}
