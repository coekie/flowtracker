package be.coekaerts.wouter.flowtracker.history;

import java.util.IdentityHashMap;
import java.util.Map;

public class StringHistory {
	private static final Map<String, StringHistory> stringToHistory
		= new IdentityHashMap<String, StringHistory>();
	
	public static final StringHistory UNKNOWN = new StringHistory(null);
	
	private StringOrigin origin;
	
	public static StringHistory getHistory(String str) {
		if (stringToHistory.containsKey(str)) {
			return stringToHistory.get(str);
		} else {
			return UNKNOWN;
		}
	}
	
	public static StringHistory createHistory(String str, StringOrigin origin) {
		if (stringToHistory.containsKey(str)) {
			throw new IllegalStateException("A history already exists for that String");
		} else {
			StringHistory tracker = new StringHistory(origin);
			stringToHistory.put(str, tracker);
			return tracker;
		}
	}
	
	private StringHistory(StringOrigin origin) {
		this.origin = origin;
	}
	
	public StringOrigin getOrigin() {
		return origin;
	}
}
