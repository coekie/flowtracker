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

import com.coekie.flowtracker.weaver.debug.RealCommentator.CommentLabel;
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
