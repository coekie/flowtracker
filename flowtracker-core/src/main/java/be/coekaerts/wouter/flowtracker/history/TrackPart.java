package be.coekaerts.wouter.flowtracker.history;

/**
 * Description of the source of a part of a {@link Tracker}. 
 */
public class TrackPart {
	private final Tracker tracker;
	private int index;
	private int length;
	
	public TrackPart(Tracker tracker, int index, int length) {
		this.tracker = tracker;
		this.index = index;
		this.length = length;
	}
	
	/**
	 * The tracker that this part points to.
	 * 
	 * This is the source; this is <b>not</b> the tracker of which this is a part.
	 */
	public Tracker getTracker() {
		return tracker;
	}
	
	/**
	 * The index in {@link #getTracker()} where this part starts
	 */
	public int getIndex() {
		return index;
	}

	void setIndex(int index) {
		this.index = index;
	}
	
	/**
	 * The length of this part
	 */
	public int getLength() {
		return length;
	}

	void setLength(int length) {
		this.length = length;
	}

}
