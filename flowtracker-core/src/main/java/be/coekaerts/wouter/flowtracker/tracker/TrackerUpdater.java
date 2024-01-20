package be.coekaerts.wouter.flowtracker.tracker;

public class TrackerUpdater {
  public static void setSource(Object target, int targetIndex, int length, Object source,
      int sourceIndex) {
    setSourceTracker(target, targetIndex, length, TrackerRepository.getTracker(source),
        sourceIndex);
  }

  public static void setSourceTracker(Object target, int targetIndex, int length,
      Tracker sourceTracker, int sourceIndex) {
    Tracker targetTracker;
    if (sourceTracker == null) {
      targetTracker = TrackerRepository.getTracker(target);
      // unknown source and unknown target; nothing to do
      if (targetTracker == null) return;
    } else {
      targetTracker = TrackerRepository.getOrCreateTracker(target);
      // if tracking is disabled, do nothing
      if (targetTracker == null) return;
    }
    targetTracker.setSource(targetIndex, length, sourceTracker, sourceIndex);
  }

  public static void setSourceTrackerPoint(Object target, int targetIndex, int length,
      TrackerPoint sourcePoint) {
    if (sourcePoint == null) {
      setSourceTracker(target, targetIndex, length, null, -1);
    } else {
      setSourceTracker(target, targetIndex, length, sourcePoint.tracker, sourcePoint.index);
    }
  }
}
