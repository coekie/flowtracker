package be.coekaerts.wouter.flowtracker.history;

import java.util.IdentityHashMap;
import java.util.Map;

abstract class HistoryKeeper<E, H> {
	private final Map<E, H> objectToHistory
		= new IdentityHashMap<E, H>();
	
	private final H unknownHistory;
	
	public HistoryKeeper(H unknownHistory) {
		this.unknownHistory = unknownHistory;
	}
	
	public H getHistory(E obj) {
		if (objectToHistory.containsKey(obj)) {
			return objectToHistory.get(obj);
		} else {
			return unknownHistory;
		}
	}
	
	public H createHistory(E obj, Origin origin) {
		if (objectToHistory.containsKey(obj)) {
			throw new IllegalStateException("A history already exists for that object");
		} else {
			H history = doCreateHistory(origin);
			objectToHistory.put(obj, history);
			return history;
		}
	}
	
	protected abstract H doCreateHistory(Origin origin);
}
