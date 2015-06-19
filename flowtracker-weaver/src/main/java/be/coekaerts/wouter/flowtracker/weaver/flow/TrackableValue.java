package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;

/** A value of which we can track where it came from */
abstract class TrackableValue extends BasicValue {
  private boolean tracked;

  TrackableValue(Type type) {
    super(type);
  }

  void ensureTracked(MethodNode methodNode) {
    if (!tracked) {
      insertTrackStatements(methodNode);
      tracked = true;
    }
  }

  /**
   * Insert the statements needed to keep track of the origin of this value.
   *
   * This method should not be called directly, instead {@link #ensureTracked(MethodNode)} should
   * be used, to ensure statements are not inserted more than once.
   *
   * @param methodNode method to add the statements in, at the right place
   */
  abstract void insertTrackStatements(MethodNode methodNode);

  /**
   * Add the tracker from which this value came on top of the stack.
   * The instructions inserted should use maximum 2 stack entries.
   *
   * @param toInsert list of instructions where the needed statements are added to at the end
   */
  abstract void loadSourceTracker(InsnList toInsert);

  /**
   * Add the index from which this value came on top of the stack
   *
   * @param toInsert list of instructions where the needed statements are added to at the end
   */
  abstract void loadSourceIndex(InsnList toInsert);
}
