package be.coekaerts.wouter.flowtracker.tracker;

import java.nio.ByteBuffer;

/**
 * Tracker for reading from a source of bytes.
 *
 * @see CharOriginTracker
 * @see ByteSinkTracker
 */
public class ByteOriginTracker extends OriginTracker implements ByteContentTracker {
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

  @Override
  public ByteSequence getContent() {
    return content;
  }
}
