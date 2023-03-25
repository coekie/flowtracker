package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.RealCommentator.CommentLabel;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;

/**
 * Textifier that handles comments added by {@link RealCommentator}
 */
public class CommentTextifier extends Textifier {
  public CommentTextifier() {
    super(Opcodes.ASM9);
  }

  @Override
  public void visitLabel(Label label) {
    if (label instanceof CommentLabel) {
      text.add("// " + ((CommentLabel) label).comment + "\n");
    } else {
      super.visitLabel(label);
    }
  }

  @Override
  protected Textifier createTextifier() {
    return new CommentTextifier();
  }
}
