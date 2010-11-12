package be.coekaerts.wouter.flowtracker.tracker;

public class FixedOriginTracker extends OriginTracker {
	private final int length;

	public FixedOriginTracker(int length) {
		this.length = length;
	}

	@Override
	public int getLength() {
		return length;
	}
	
	@Override
	public boolean canGrow() {
		return false;
	}
}
