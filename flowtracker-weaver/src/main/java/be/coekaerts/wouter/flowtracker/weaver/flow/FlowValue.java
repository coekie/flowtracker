package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import java.util.Objects;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * A value in our flow analysis. That is, a representation of what we know about a certain value
 * (on the stack or in a local variable) at a certain point in the execution of a method.
 */
abstract class FlowValue extends BasicValue {
  private FlowFrame creationFrame;

  FlowValue(Type type) {
    super(type);
  }

  /**
   * Insert the statements needed to keep track of the origin of this value, if applicable.
   * Idempotent: may be called multiple times; tracking should only be setup once.
   */
  abstract void ensureTracked();

  /**
   * Returns if we can know where this value came from.
   * <p>
   * If this is false, then {@link #loadSourcePoint(InsnList)} will always load {@code null}.
   * If this return true, then that's no guarantee that {@link #loadSourcePoint(InsnList)} is
   * never {@code null}.
   */
  abstract boolean isTrackable();

  /**
   * The instruction at which this value was last touched (created, copied or merged), or null if
   * unknown.
   */
  abstract AbstractInsnNode getCreationInsn();

  /**
   * The {@link FlowFrame} that the analyzer built for {@link #getCreationInsn()}
   */
  FlowFrame getCreationFrame() {
    return creationFrame;
  }

  void initCreationFrame(FlowAnalyzer analyzer) {
    AbstractInsnNode creationInsn = getCreationInsn();
    if (creationInsn != null) {
      creationFrame = analyzer.getFrame(creationInsn);
    }
  }

  /**
   * Add the {@link TrackerPoint} from which this value came on top of the stack.
   * The instructions inserted should use maximum 2 stack entries.
   *
   * @param toInsert list of instructions where the needed statements are added to at the end
   */
  abstract void loadSourcePoint(InsnList toInsert);

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o.getClass() != this.getClass()) {
      return false;
    } else {
      return Objects.equals(getType(), ((FlowValue) o).getType());
    }
  }

  @Override
  public int hashCode() {
    return getType() == null ? 0 : getType().hashCode();
  }

  /**
   * Returns if this value somewhere (through merges and copies) contains a MergeValue at the given
   * mergingFrame. Used to detect loops in the data flow.
   */
  abstract boolean hasMergeAt(FlowFrame mergingFrame);
}
