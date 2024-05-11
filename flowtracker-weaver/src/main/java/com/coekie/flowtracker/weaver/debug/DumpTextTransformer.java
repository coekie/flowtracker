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

import com.coekie.flowtracker.weaver.Transformer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

/** Dumps instrumented code in text format. Enabled using the dumpText option */
public class DumpTextTransformer implements Transformer {
  private final Transformer delegate;
  private final File dumpTextPath;

  public DumpTextTransformer(Transformer delegate, File dumpTextPath) {
    this.delegate = delegate;
    this.dumpTextPath = dumpTextPath;
  }

  @Override
  public ClassVisitor transform(String className, ClassVisitor cv) {
    try {
      String fileName = className.replaceAll("[/.$]", "_") + ".asm";
      FileOutputStream out = new FileOutputStream(new File(dumpTextPath, fileName));
      PrintWriter pw = new PrintWriter(out);
      TraceClassVisitor traceClassVisitor = new TraceClassVisitor(cv, new CommentTextifier(), pw);
      ClassVisitor closingVisitor = new ClassVisitor(Opcodes.ASM9, traceClassVisitor) {
        @Override
        public void visitEnd() {
          super.visitEnd();
          pw.close();
        }
      };
      return delegate.transform(className, closingVisitor);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
