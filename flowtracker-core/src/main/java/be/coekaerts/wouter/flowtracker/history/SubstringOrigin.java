package be.coekaerts.wouter.flowtracker.history;

public class SubstringOrigin implements Origin {
	private final String source;
	private final int beginIndex;
	private final int endIndex;
	
	public SubstringOrigin(String source, int beginIndex, int endIndex) {
		this.source = source;
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
	}

	public String getSource() {
		return source;
	}

	public int getBeginIndex() {
		return beginIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}
}
