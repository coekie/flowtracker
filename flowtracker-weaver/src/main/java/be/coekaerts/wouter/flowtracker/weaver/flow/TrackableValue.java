package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/** A value of which we can track where it came from */
abstract class TrackableValue extends FlowValue {
  final FlowMethodAdapter flowMethodAdapter;
  private final AbstractInsnNode insn;
  private boolean tracked;

  TrackableValue(FlowMethodAdapter flowMethodAdapter, Type type, AbstractInsnNode insn) {
    super(type);
    this.flowMethodAdapter = flowMethodAdapter;
    this.insn = insn;
  }

  void ensureTracked() {
    if (!tracked) {
      insertTrackStatements();
      tracked = true;
    }
  }

  @Override
  public boolean equals(Object o) {
    return o == this || (super.equals(o) && ((TrackableValue) o).insn == this.insn);
  }

  /**
   * Insert the statements needed to keep track of the origin of this value.
   * <p>
   * This method should not be called directly, instead {@link #ensureTracked()}
   * should be used, to ensure statements are not inserted more than once.
   */
  abstract void insertTrackStatements();

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
