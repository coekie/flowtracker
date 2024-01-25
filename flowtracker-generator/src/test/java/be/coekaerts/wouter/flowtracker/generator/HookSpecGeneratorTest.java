package be.coekaerts.wouter.flowtracker.generator;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.Test;

public class HookSpecGeneratorTest {
  @Test
  public void generatedCodeUpToDate() throws IOException {
    assertEquals(
        removeStaticImports(Files.readString(HookSpecGenerator.OUTPUT_FILE)),
        removeStaticImports(HookSpecGenerator.generate()));
  }

  // don't fail if IntelliJ optimized the imports
  private String removeStaticImports(String code) {
    return code.replaceAll("import static [^;]*;\n", "");
  }
}