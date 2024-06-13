package demo;

import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class AsmDemo {
  public static void main(String... args) throws IOException {
    ClassWriter cw = new ClassWriter(0);
    new ClassReader(HelloWorld.class.getResourceAsStream("HelloWorld.class"))
        .accept(cw, 0);
    System.out.write(cw.toByteArray());
  }
}
