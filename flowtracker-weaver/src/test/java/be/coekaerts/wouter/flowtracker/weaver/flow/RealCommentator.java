package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

/** Commentator that adds comments by creating labels */
class RealCommentator extends Commentator {
  @Override
  public void addComment(InsnList insnList, String comment, Object... commentArgs) {
    insnList.add(new LabelNode(new CommentLabel(String.format(comment, commentArgs))));
  }

  /** Label (ab)used to represent a comment */
  static class CommentLabel extends Label {
    final String comment;

    CommentLabel(String comment) {
      this.comment = comment;
    }
  }
}
