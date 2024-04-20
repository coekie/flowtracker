package be.coekaerts.wouter.flowtracker.generator;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.Test;

public class HookSpecGeneratorTest {
  @Test
  public void generatedCodeUpToDate() throws IOException {
    assertThat(removeStaticImports(HookSpecGenerator.generate()))
        .isEqualTo(removeStaticImports(Files.readString(HookSpecGenerator.OUTPUT_FILE)));
  }

  // don't fail if IntelliJ optimized the imports
  private String removeStaticImports(String code) {
    return code.replaceAll("import static [^;]*;\n", "");
  }
}