package com.coekie.flowtracker.weaver.debug;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.coekie.flowtracker.weaver.flow.Commentator;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

/** Commentator that adds comments by creating labels */
public class RealCommentator extends Commentator {
  @Override
  public void addComment(InsnList insnList, String comment, Object... commentArgs) {
    insnList.add(new LabelNode(new CommentLabel(String.format(comment, commentArgs))));
  }

  /** Label (ab)used to represent a comment */
  public static class CommentLabel extends Label {
    final String comment;

    CommentLabel(String comment) {
      this.comment = comment;
    }
  }
}
