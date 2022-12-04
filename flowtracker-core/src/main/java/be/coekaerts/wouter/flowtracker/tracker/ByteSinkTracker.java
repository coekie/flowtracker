package be.coekaerts.wouter.flowtracker.tracker;

import java.nio.ByteBuffer;

/**
 * Tracker for writing to a sink of bytes.
 *
 * @see ByteOriginTracker
 * @see CharSinkTracker
 */
public class ByteSinkTracker extends DefaultTracker implements ByteContentTracker {
  private final ByteSequence content = new ByteSequence();

  @Override
  public int getLength() {
    return content.size();
  }

  public void append(byte b) {
    content.write(b);
  }

  public void append(byte[] cbuf, int offset, int len) {
    content.write(cbuf, offset, len);
  }

  public ByteBuffer getByteContent() {
    return content.getByteContent();
  }

  @Override
  public ByteSequence getContent() {
    return content;
  }
}
