package be.coekaerts.wouter.flowtracker.history;

public class History {
	private final Origin origin;

	public History(Origin origin) {
		this.origin = origin;
	}
	
	public Origin getOrigin() {
		return origin;
	}
}
