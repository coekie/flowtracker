package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Tracker that just contains a descriptor. No tracking of content is done.
 *
 * Arguably this class shouldn't actually extend Tracker; but then they'd have to be stored
 * somewhere else than the {@link TrackerRepository}.
 */
public class TagTracker extends Tracker {
  public TagTracker(String descriptor) {
    initDescriptor(descriptor, null);
  }

  @Override public int getEntryCount() {
    throw new UnsupportedOperationException();
  }

  @Override public int getLength() {
    throw new UnsupportedOperationException();
  }
}
