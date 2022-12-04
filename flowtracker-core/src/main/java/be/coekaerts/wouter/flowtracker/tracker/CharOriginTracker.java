package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Tracker for reading from a source of chars or Strings.
 *
 * @see ByteOriginTracker
 * @see CharSinkTracker
 */
public class CharOriginTracker extends OriginTracker implements CharContentTracker {

  private final StringBuilder content = new StringBuilder();

  public void append(char c) {
    content.append(c);
  }

  public void append(char[] cbuf, int offset, int len) {
    content.append(cbuf, offset, len);
  }

  public void append(CharSequence charSequence) {
    content.append(charSequence);
  }

  @Override public CharSequence getContent() {
    return content;
  }

  @Override
  public int getLength() {
    return content.length();
  }
}
