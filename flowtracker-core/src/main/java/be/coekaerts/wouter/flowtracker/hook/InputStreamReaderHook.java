package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStreamReader;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class InputStreamReaderHook {
	
	public static void afterRead1(int result, InputStreamReader target) {
		if (Trackers.isActive() && result > 0) {
			ContentTracker tracker = TrackerRepository.getOrCreateContentTracker(target);
			tracker.append((char) result);
		}
	}
	
	// SHORTCUT: assume Reader.read(char[]) delegates to read(cbuf, 0, cbuf.length) 
//	public static void afterReadCharArray(int read, InputStreamReader target, char[] cbuf) {
//		afterReadCharArrayOffset(read, target, cbuf, 0);
//	}
	
	public static void afterReadCharArrayOffset(int read, InputStreamReader target, char[] cbuf, int offset) {
		if (Trackers.isActive() && read >= 0) {
			ContentTracker tracker = TrackerRepository.getOrCreateContentTracker(target);
			tracker.append(cbuf, offset, read);
		}
	}
	
	// SHORTCUT: int read(CharBuffer):
	//  assume super implementation will delegate to another read method that we already handle
	// alternatives:
	// * delegate to another read method ourselves
	// * get the result back using target.subsequence()
}
