package com.coekie.flowtracker.weaver;

import com.coekie.flowtracker.util.Config;
import org.junit.Test;

public class GeneratedHookSpecsTest {
  @Test
  public void test() {
    GeneratedHookSpecs.createTransformer(Config.empty()).typeCheck();
  }
}