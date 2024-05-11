package com.coekie.flowtracker.weaver.flow;

import org.objectweb.asm.tree.InsnList;

/**
 * Supports adding extra instructions representing comments; useful to understand and debug our
 * transformations. This implementation does nothing but is overridden in tests.
 */
public class Commentator {
  public void addComment(InsnList insnList, String comment, Object... commentArgs) {
  }
}
