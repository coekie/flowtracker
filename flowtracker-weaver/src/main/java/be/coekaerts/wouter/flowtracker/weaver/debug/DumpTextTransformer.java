package be.coekaerts.wouter.flowtracker.weaver.debug;

import be.coekaerts.wouter.flowtracker.weaver.ClassAdapterFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

/** Dumps instrumented code in text format. Enabled using the dumpText option */
public class DumpTextTransformer implements ClassAdapterFactory {
  private final ClassAdapterFactory delegate;
  private final String className;
  private final File dumpTextPath;

  public DumpTextTransformer(ClassAdapterFactory delegate, String className, File dumpTextPath) {
    this.delegate = delegate;
    this.className = className;
    this.dumpTextPath = dumpTextPath;
  }

  @Override
  public ClassVisitor createClassAdapter(ClassVisitor cv) {
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
      return delegate.createClassAdapter(closingVisitor);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
