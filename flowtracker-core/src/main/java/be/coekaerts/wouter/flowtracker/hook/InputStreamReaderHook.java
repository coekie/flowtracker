package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.InputStream;
import java.io.InputStreamReader;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class InputStreamReaderHook {

  public static void afterInit(InputStreamReader target, InputStream source) {
    TrackerRepository.createContentTracker(target).initDescriptor("InputStreamReader", source);
  }

	public static void afterRead1(int result, InputStreamReader target) {
		if (Trackers.isActive() && result > 0) {
			ContentTracker tracker = TrackerRepository.getContentTracker(target);
      if (tracker != null) {
			  tracker.append((char) result);
      }
		}
	}
	
	public static void afterReadCharArrayOffset(int read, InputStreamReader target, char[] cbuf, int offset) {
		if (Trackers.isActive() && read >= 0) {
			ContentTracker tracker = TrackerRepository.getContentTracker(target);
      if (tracker != null) {
			  tracker.append(cbuf, offset, read);
      }
		}
	}
}
