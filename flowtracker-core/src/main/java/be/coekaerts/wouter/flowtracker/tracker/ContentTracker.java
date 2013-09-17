package be.coekaerts.wouter.flowtracker.tracker;

public class ContentTracker extends OriginTracker {

	private StringBuilder content = new StringBuilder();

  public void append(char c) {
		content.append(c);
	}

	public void append(char[] cbuf, int offset, int len) {
		content.append(cbuf, offset, len);
	}

  public void append(String str, int offset, int len) {
    content.append(str, offset, offset + len);
  }
	
	public CharSequence getContent() {
		return content;
	}

  @Override
	public int getLength() {
		return content.length();
	}
}
