package com.coekie.flowtracker.weaver.flow;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;

/**
 * {@link LocalVariablesSorter} that can be bypassed.
 *
 * <p>LocalVariablesSorter has a slightly weird API: its javadoc notes that the preferred way to
 * use it is that "the next visitor in the chain can indeed add new locals when needed by calling
 * {@link #newLocal} on this adapter (this requires a reference back to this
 * {@link LocalVariablesSorter}". But that doesn't work out when we are doing transformations on a
 * {@link MethodNode}; because there we wait until we've gotten the full method implementation
 * before analyzing, transforming, and passing it to the next node. At that point it's too late to
 * still call {@link #newLocal} on a visitor that came <em>before</em> us. So instead we put the
 * LocalVariablesSorter after our transformation; but our added nodes that refer to new local
 * variables bypass it (to avoid those from getting translated).
 */
class TransparentLocalVariablesSorter extends LocalVariablesSorter {
  TransparentLocalVariablesSorter(int access, String descriptor, MethodVisitor methodVisitor) {
    super(Opcodes.ASM9, access, descriptor, methodVisitor);
  }

  static MethodVisitor bypass(MethodVisitor mv) {
    return ((TransparentLocalVariablesSorter) mv).mv;
  }

  @Override
  public void visitCode() {
    super.visitCode();
  }
}
