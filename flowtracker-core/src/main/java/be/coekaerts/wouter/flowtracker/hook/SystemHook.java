package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class SystemHook {
	@SuppressWarnings("SuspiciousSystemArraycopy")
	public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		Tracker.setSource(dest, destPos, length, src, srcPos);
	}

  public static void initialize() {
    TrackerRepository.createContentTracker(System.out).initDescriptor("System.out", null);
    TrackerRepository.createContentTracker(System.err).initDescriptor("System.err", null);
    TrackerRepository.createContentTracker(System.in).initDescriptor("System.in", null);
  }
}
