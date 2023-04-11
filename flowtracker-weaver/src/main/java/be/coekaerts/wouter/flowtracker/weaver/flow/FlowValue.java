package be.coekaerts.wouter.flowtracker.weaver.flow;

import java.util.Objects;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * A value in our flow analysis. That is, a representation of what we know about a certain value
 * (on the stack or in a local variable) at a certain point in the execution of a method.
 */
abstract class FlowValue extends BasicValue {

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
   * If this is false, then {@link #loadSourceTracker(InsnList)} will always load {@code null}.
   * If this return true, then that's no guarantee that {@link #loadSourceTracker(InsnList)} is
   * never {@code null}.
   */
  abstract boolean isTrackable();

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
}
