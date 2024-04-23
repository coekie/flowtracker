package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.util.Config;
import org.junit.Test;

public class GeneratedHookSpecsTest {
  @Test
  public void test() {
    GeneratedHookSpecs.createTransformer(Config.empty()).typeCheck();
  }
}