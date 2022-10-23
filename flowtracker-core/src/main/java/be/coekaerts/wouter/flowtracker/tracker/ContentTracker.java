package be.coekaerts.wouter.flowtracker.tracker;

public class ContentTracker extends OriginTracker {

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

  @Override public boolean supportsContent() {
    return true;
  }

  @Override public CharSequence getContent() {
    return content;
  }

  @Override
  public int getLength() {
    return content.length();
  }
}
