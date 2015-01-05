package be.coekaerts.wouter.flowtracker.tracker;

public class SinkTracker extends DefaultTracker {
  private final StringBuilder content = new StringBuilder();

  @Override public CharSequence getContent() {
    return content;
  }

  public void append(char c) {
    content.append(c);
  }

  public void append(char[] cbuf, int off, int len) {
    content.append(cbuf, off, len);
  }

  public void append(String str, int off, int len) {
    content.append(str, off, off + len);
  }
}
