package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.history.Tracker;

public class CharArrayHook {
	public static void setCharWithOrigin(char[] array, int arrayIndex, char value, Object source, int sourceIndex) {
		array[arrayIndex] = value;
		
		Tracker.setSource(array, arrayIndex, 1, source, sourceIndex);
	}
}
