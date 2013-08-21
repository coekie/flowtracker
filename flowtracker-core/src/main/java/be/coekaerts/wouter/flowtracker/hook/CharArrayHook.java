package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class CharArrayHook {
	public static void setCharWithOrigin(char[] array, int arrayIndex, char value, Tracker source, int sourceIndex) {
		array[arrayIndex] = value;
		
		Tracker.setSourceTracker(array, arrayIndex, 1, source, sourceIndex);
	}
}
