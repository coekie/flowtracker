package be.coekaerts.wouter.flowtracker.tracker;

public class ContentTracker extends OriginTracker {
  private final String descriptor;
  private final Tracker descriptorTracker;

	private StringBuilder content = new StringBuilder();

  public ContentTracker(String descriptor, Tracker descriptorTracker) {
    this.descriptor = descriptor;
    this.descriptorTracker = descriptorTracker;
  }

  public void append(char c) {
		content.append(c);
	}

	public void append(char[] cbuf, int offset, int len) {
		content.append(cbuf, offset, len);
	}
	
	public CharSequence getContent() {
		return content;
	}

  public String getDescriptor() {
    return descriptor;
  }

  public Tracker getDescriptorTracker() {
    return descriptorTracker;
  }

  @Override
	public int getLength() {
		return content.length();
	}
}
