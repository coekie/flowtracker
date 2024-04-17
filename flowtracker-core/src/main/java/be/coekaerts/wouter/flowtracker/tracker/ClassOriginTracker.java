package be.coekaerts.wouter.flowtracker.tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracker for constants (literals) defined in a class.
 * <p>
 * Ideally we'd have a proper model of class members. We could then render that, when requested.
 * Then members could be added only if they've actually being used (tracked). We could even render
 * it in a nicer way in the UI.
 * But for now we represent the whole class as a long String, built at instrumentation time, that
 * contains everything that _could_ be tracked. That's simpler to implement.
 */
public class ClassOriginTracker extends OriginTracker implements CharContentTracker {
  private static final List<ClassOriginTracker> trackers = new ArrayList<>();

  public final int classId;
  private final StringBuilder content = new StringBuilder();

  private ClassOriginTracker(String className) {
    this.classId = trackers.size();
    content.append("class ").append(className).append('\n');
  }

  public static synchronized ClassOriginTracker registerClass(String className) {
    ClassOriginTracker tracker = new ClassOriginTracker(className);
    trackers.add(tracker);
    tracker.addTo(TrackerTree.CLASS.pathNode(className));
    return tracker;
  }

  public static ClassOriginTracker get(int classId) {
    return trackers.get(classId);
  }

  @Override
  public CharSequence getContent() {
    return content;
  }

  @Override
  public int getLength() {
    return 0;
  }

  public synchronized void startMethod(String method) {
    content.append(method).append(":\n");
  }

  public synchronized int registerConstant(int value) {
    content.append("  ");
    int offset = content.length();
    content.append((char) value).append('\n');
    return offset;
  }

  public synchronized int registerConstantString(String value) {
    content.append("  ");
    int offset = content.length();
    content.append(value).append('\n');
    return offset;
  }
}
