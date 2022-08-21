package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * A local variable that we are adding into an instrumented method, to remember a tracker or index.
 *
 * <p>The index for this variable must come from {@link TransparentLocalVariablesSorter}. We create
 * special instruction nodes to {@link #load()} and {@link #store()} this local variable, because
 * those must bypass the {@link TransparentLocalVariablesSorter}. That is because the sorter remaps
 * indexes of local variables assuming they are coming from the original code; we don't want our
 * own local variables to get remapped. Note that the goal of the sorter is to remap the local
 * variables of the original code to make room for our local variables, our TrackLocals.
 */
class TrackLocal {
  private final Type type;

  /** Index of this local variable */
  private final int index;

  TrackLocal(Type type, int index) {
    this.type = type;
    this.index = index;
  }

  /** Create a node for an instruction to load this local variable */
  VarInsnNode load() {
    return createVarInsnNode(type.getOpcode(Opcodes.ILOAD));
  }

  /** Create a node for an instruction to store this local variable */
  VarInsnNode store() {
    return createVarInsnNode(type.getOpcode(Opcodes.ISTORE));
  }

  private VarInsnNode createVarInsnNode(int opcode) {
    return new VarInsnNode(opcode, index) {
      @Override
      public void accept(MethodVisitor methodVisitor) {
        super.accept(TransparentLocalVariablesSorter.bypass(methodVisitor));
      }
    };
  }
}
