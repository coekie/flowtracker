package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;

/** Manages transformation for constant values ({@link ConstantValue}) */
class ConstantsTransformation {
  private final String className;
  private ClassOriginTracker tracker;

  ConstantsTransformation(String className) {
    this.className = className;
  }

  /**
   * Lazily initializes the {@link ClassOriginTracker}, so that only classes that actually have
   * something to track get registered.
   */
  private ClassOriginTracker tracker() {
    if (tracker == null) {
      tracker = ClassOriginTracker.registerClass(className);
    }
    return tracker;
  }

  int trackConstant(String method, int value) {
    return tracker().registerConstant(method, value);
  }

  int classId() {
    return tracker().classId;
  }
}
